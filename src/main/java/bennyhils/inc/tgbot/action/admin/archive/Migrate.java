package bennyhils.inc.tgbot.action.admin.archive;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.WireGuardClient;
import bennyhils.inc.tgbot.model.WireGuardServer;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import bennyhils.inc.tgbot.vpn.WireGuardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Migrate implements Action {


    private final Properties properties;

    private final WireGuardService wireGuardService = new WireGuardService();

    private final OutlineService outlineService = new OutlineService();

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public Migrate(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        Map<String, WireGuardServer> allWireGuardClientsToServerMap = wireGuardService.getServersClients(properties);
        List<WireGuardClient> allWGWireGuardClients = new ArrayList<>();
        for (String server : allWireGuardClientsToServerMap.keySet()) {
            allWGWireGuardClients.addAll(allWireGuardClientsToServerMap.get(server).getWireGuardClients());
        }

        return new SendMessage(
                update.getMessage().getChatId().toString(),
                "Сколько клиентов хотите мигрировать?" +
                "\n\nВсего их " + allWGWireGuardClients.size()
        );
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        Map<String, WireGuardServer> allWireGuardClientsToServerMap = wireGuardService.getServersClients(properties);
        List<WireGuardClient> allWGWireGuardClients = new ArrayList<>();
        for (String server : allWireGuardClientsToServerMap.keySet()) {
            allWGWireGuardClients.addAll(allWireGuardClientsToServerMap.get(server).getWireGuardClients());
        }
        List<String> servers = new ArrayList<>();
        try {
            servers = OBJECT_MAPPER.readValue(
                    properties.getProperty("servers.outline"),
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            log.error("Не удалось получить список серверов: '{}'", e.getMessage());
        }
        long allMigrationTime = 0;
        List<String> outlineClientsTgIds = outlineService
                .getAllServersClients(properties)
                .stream()
                .map(OutlineClient::getName).toList();
        List<WireGuardClient> wireGuardClientsForMigration = new ArrayList<>();
        int count;
        try {
            count = Integer.parseInt(update.getMessage().getText());
        } catch (NumberFormatException e) {
            log.warn("Введено не число!");

            return new SendMessage(update.getMessage().getChatId().toString(), "Миграция отменена. Введите число!");
        }
        if (count > allWGWireGuardClients.size()) {
            count = allWGWireGuardClients.size();
        }
        int j = 0;
        for (int r = 0; r < count; r++) {
            WireGuardClient wireGuardClient = allWGWireGuardClients.get(r);
            if (outlineClientsTgIds.contains(wireGuardClient.getTgId())) {
                log.info("Миграция клиента '{}' не требуется", wireGuardClient.getTgId());
                j = r + 1;
//                wireGuardClientsForMigration.add(wireGuardClient);
            } else {
                wireGuardClientsForMigration.add(wireGuardClient);
            }
        }

        int i = 0;
        int k = 0;
        for (String s : servers) {
            if (s.equals(outlineService.findServerForClientCreation(properties))) {
                i = k;
                break;
            }
            k++;
        }

        for (WireGuardClient wireGuardClient : wireGuardClientsForMigration) {
            long start = System.currentTimeMillis();
            log.info(
                    ">>>>>>> НАЧАЛО МИГРАЦИИ КЛИЕНТА {} из {} -> '{}', '{}'",
                    j + 1,
                    allWGWireGuardClients.size(),
                    wireGuardClient.getTgLogin(),
                    wireGuardClient.getTgId()
            );
            OutlineClient createdClient = outlineService.createClient(
                    servers.get(i),
                    Integer.parseInt(properties.getProperty("free.days.period")),
                    wireGuardClient.getTgId(),
                    wireGuardClient.getTgLogin(),
                    wireGuardClient.getTgFirst(),
                    wireGuardClient.getTgLast()
            );
            log.info(
                    "Создан клиент '{}' на севере '{}' за '{}' сек.",
                    createdClient.getTgLogin(),
                    i,
                    TimeUnit.MILLISECONDS.toSeconds(
                            System.currentTimeMillis() - start)
            );
            Instant newPaidBefore = wireGuardClient.getPaidBefore().isBefore(Instant.now()) ?
                    Instant
                            .now()
                            .plus(Long.parseLong(properties.getProperty("free.days.period")), ChronoUnit.DAYS) :
                    wireGuardClient
                            .getPaidBefore()
                            .plus(Long.parseLong(properties.getProperty("free.days.period")), ChronoUnit.DAYS);
            outlineService.updatePaidBefore(
                    servers.get(i),
                    newPaidBefore,
                    createdClient.getId().toString()
            );
            log.info(
                    "У клиента '{}' обновлена оплата до '{}'",
                    createdClient.getTgLogin(),
                    DataTimeUtil.getNovosibirskTimeFromInstant(newPaidBefore)
            );
            outlineService.updateCreatedAtAndUpdatedAt(
                    servers.get(i),
                    createdClient.getId().toString(),
                    wireGuardClient.getCreatedAt().minus(2L, ChronoUnit.DAYS),
                    wireGuardClient.getUpdatedAt()
            );
            log.info(
                    "У клиента '{}' обновлены даты создания и обновления на: '{}', '{}'",
                    createdClient.getTgLogin(),
                    DataTimeUtil.getNovosibirskTimeFromInstant(wireGuardClient
                            .getCreatedAt()
                            .minus(2L, ChronoUnit.DAYS)),
                    DataTimeUtil.getNovosibirskTimeFromInstant(wireGuardClient.getUpdatedAt())
            );
            long migrationTime = System.currentTimeMillis() - start;
            allMigrationTime = allMigrationTime + migrationTime;
            log.info(
                    "Текущая миграция заняла '{}' сек., а общая идет уже {} мин.",
                    TimeUnit.MILLISECONDS.toSeconds(migrationTime),
                    TimeUnit.MILLISECONDS.toMinutes(allMigrationTime)
            );
            log.info(
                    "<<<<<<< КОНЕЦ МИГРАЦИИ КЛИЕНТА '{}', '{}'",
                    wireGuardClient.getTgLogin(),
                    wireGuardClient.getTgId()
            );
            j++;
            i = j % servers.size();
        }
        String msg = "Мигрировало '" +
                     wireGuardClientsForMigration.size() +
                     "' клиентов";

        return new SendMessage(update.getMessage().getChatId().toString(), msg);
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {

        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {

        return null;
    }

    @Override
    public Map<Long, List<PartialBotApiMethod<Message>>> sendMassMessages(Update update) {

        return null;
    }
}
