package bennyhils.inc.tgbot.vpn;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    public Map<String, Long> getDataUsage(Properties properties) {
        Map<String, OutlineServer> outlineServersWithClientsMap = getOutlineServersWithClientsMap(properties);
        Map<String, Long> dataUsage = new HashMap<>();
        for (String s : outlineServersWithClientsMap.keySet()) {
            dataUsage.putAll(outlineHttpClient.getDataUsage(s));
            for (OutlineClient c : outlineServersWithClientsMap.get(s).getClients()) {
                dataUsage.put(c.getName(), dataUsage.get(c.getId().toString()));
                dataUsage.remove(c.getId().toString());
            }
        }

        return dataUsage;
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

    public void updateTgData(
            String server,
            String id,
            String tgLogin,
            String tgFirst,
            String tgLast
    ) {
        outlineHttpClient.updateClientTgData(
                server,
                id,
                tgLogin,
                tgFirst,
                tgLast
        );

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

            return null;
        }

        Map<String, OutlineServer> allServersOutlineClients = new HashMap<>();
        for (String server : servers) {
            List<OutlineClient> clients = outlineHttpClient.getClients(server);
            allServersOutlineClients.put(
                    server,
                    new OutlineServer(
                            clients.size(),
                            clients.stream().map(OutlineClient::getName).collect(
                                    Collectors.toSet()),
                            clients
                    )
            );
        }

        return allServersOutlineClients;
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
