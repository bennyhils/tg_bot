package bennyhils.inc.tgbot.vpn;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.model.ServerOutlineNative;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class OutlineService implements VPNService {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    OutlineHttpClient outlineHttpClient = new OutlineHttpClient();

    @Override
    public List<OutlineClient> getAllServersClients(Properties properties) {
        Map<String, OutlineServer> allServersOutlineClients = getOutlineServersWithClientsMap(properties);
        List<OutlineClient> outlineClients = new ArrayList<>();
        for (String outlineServer : allServersOutlineClients.keySet()) {
            outlineClients.addAll(allServersOutlineClients.get(outlineServer).getClients());
        }

        return outlineClients;
    }

    public Map<String, Long> getDataUsage(Map<String, OutlineServer> outlineServerConfigs) {

        return getDataUsageParallel(outlineServerConfigs);
    }

    public Map<String, OutlineClient> getClientByTgId(Map<String, OutlineServer> outlineServersMap, String tgId) {
        Map<String, OutlineClient> outlineClientMap = new HashMap<>();
        for (String server : outlineServersMap.keySet()) {
            OutlineClient existingClient = outlineServersMap
                    .get(server)
                    .getClients()
                    .stream()
                    .filter(outlineClient -> outlineClient.getName().equals(tgId))
                    .findFirst()
                    .orElse(null);

            if (existingClient != null) {
                outlineClientMap.put(server, existingClient);
                break;
            }
        }

        return outlineClientMap;
    }

    @Override
    public OutlineClient createClient(
            String server,
            int freeDaysPeriod,
            String tgId,
            String tgLogin,
            String tgFirst,
            String tgLast
    ) {
        OutlineClient outlineClient = outlineHttpClient.createClient(server);
        outlineHttpClient.renameClient(server, outlineClient.getId().toString(), tgId);
        outlineHttpClient.updateClientTgData(
                server,
                outlineClient.getId().toString(),
                tgLogin,
                tgFirst,
                tgLast
        );
        outlineHttpClient.updatePaidBefore(server, outlineClient.getId().toString(), outlineClient
                .getPaidBefore()
                .plus(freeDaysPeriod, ChronoUnit.DAYS));

        return outlineHttpClient.getClient(server, outlineClient.getId().toString());
    }

    @Override
    public void updatePaidBefore(String server, Instant paidBefore, String id) {
        outlineHttpClient.updatePaidBefore(server, id, paidBefore);
    }


    @Override
    public void deleteClient(String server, String id) {
        outlineHttpClient.deleteClient(server, id);
    }

    @Override
    public void disableClient(String server, String id) {
        outlineHttpClient.setAccessKeyDataLimit(server, id);
    }

    @Override
    public void enableClient(String server, String id) {
        outlineHttpClient.removeAccessKeyDataLimit(server, id);
    }

    public void updateCreatedAtAndUpdatedAt(String server, String id, Instant createdAt, Instant updatedAt) {
        outlineHttpClient.updateCreatedAtAndUpdatedAt(server, id, createdAt, updatedAt);
    }

    public ServerOutlineNative getServerNative(String server) {
        return outlineHttpClient.getServerNative(server);
    }

    public void setPortForNewAccessKeys(String server, int port) {
        outlineHttpClient.setPortForNewAccessKeys(server, port);
    }

    public Map<String, OutlineServer> getOutlineServersWithClientsMap(Properties properties) {
        String serversString = properties.getProperty("servers.outline");
        List<String> servers = null;
        try {
            servers = OBJECT_MAPPER.readValue(serversString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось получить список серверов в ошибкой: '{}'", e.getMessage());
        }
        if (servers == null || servers.isEmpty()) {
            log.error("Не задан ни один сервер Outline для работы бота");

            return Collections.emptyMap(); // или throw new IllegalStateException();
        }

        return getAllServersOutlineClientsParallel(servers);
    }

    private Map<String, OutlineServer> getAllServersOutlineClientsParallel(List<String> servers) {
        int threadPoolSize = servers.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        try {

            // 1. Потокобезопасная мапа для результатов
            ConcurrentMap<String, OutlineServer> serversData = new ConcurrentHashMap<>();

            // 2. Создаем список Future для отслеживания задач
            List<Future<?>> futures = new ArrayList<>();

            // 3. Параллельная обработка серверов
            for (String server : servers) {
                futures.add(executorService.submit(() -> {
                    try {
                        // 3.1. Получаем клиентов сервера
                        List<OutlineClient> clients = outlineHttpClient.getClients(server);

                        // 3.2. Создаем объект OutlineServer
                        OutlineServer outlineServer = new OutlineServer(
                                clients.size(),
                                clients.stream().map(OutlineClient::getName).collect(Collectors.toSet()),
                                clients
                        );

                        // 3.3. Сохраняем результат
                        serversData.put(server, outlineServer);

                    } catch (Exception e) {
                        // Логируем ошибку, но продолжаем обработку остальных серверов
                        log.error("Ошибка при обработке сервера " + server + ": " + e.getMessage());
                    }
                }));
            }

            // 4. Ожидаем завершения всех задач
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    log.error("Ошибка выполнения задачи: " + e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано ожидание завершения задач", e);
                }
            }

            return serversData;
        } finally {
            executorService.shutdown();
        }
    }

    private Map<String, Long> getDataUsageParallel(Map<String, OutlineServer> outlineServersWithClientsMap) {
        int threadPoolSize = outlineServersWithClientsMap.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        try {
            // 2. Потокобезопасная мапа для результатов
            ConcurrentMap<String, Long> dataUsage = new ConcurrentHashMap<>();
            // 3. Создаем список Future для отслеживания задач
            List<Future<?>> futures = new ArrayList<>();
            // 4. Параллельная обработка серверов
            for (String server : outlineServersWithClientsMap.keySet()) {
                futures.add(executorService.submit(() -> {
                    // 4.1. Получаем данные использования для сервера
                    Map<String, Long> serverUsage = outlineHttpClient.getDataUsage(server);
                    dataUsage.putAll(serverUsage);

                    // 4.2. Обрабатываем клиентов этого сервера
                    OutlineServer serverData = outlineServersWithClientsMap.get(server);
                    processClients(dataUsage, serverData.getClients());
                }));
            }
            // 5. Ожидаем завершения всех задач
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    // Логируем ошибку, но продолжаем обработку остальных задач
                    log.error("Ошибка при обработке сервера: " + e.getCause().getMessage());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return dataUsage;
        } finally {
            executorService.shutdown();
        }
    }

    private void processClients(ConcurrentMap<String, Long> dataUsage, List<OutlineClient> clients) {
        // Обработка клиентов с использованием параллельного стрима
        clients.forEach(client -> {
            dataUsage.computeIfPresent(client.getId().toString(),
                    (id, usage) -> {
                        dataUsage.put(client.getName(), usage);
                        return null;
                    });
        });
    }

    public String findServerForClientCreation(Properties properties) {
        Map<String, OutlineServer> beforeCreationServersClientsMap = getOutlineServersWithClientsMap(properties);
        String serverForClientCreation = beforeCreationServersClientsMap.keySet().stream().findFirst().orElseThrow();
        for (String s : beforeCreationServersClientsMap.keySet()) {
            if (beforeCreationServersClientsMap.get(s).getClientsCount() <
                    beforeCreationServersClientsMap.get(serverForClientCreation).getClientsCount()) {
                // Сервер, где еще меньше клиентов
                serverForClientCreation = s;
            }
        }

        return serverForClientCreation;
    }
}
