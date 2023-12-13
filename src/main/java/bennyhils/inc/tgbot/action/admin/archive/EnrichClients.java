package bennyhils.inc.tgbot.action.admin.archive;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.model.WireGuardClient;
import bennyhils.inc.tgbot.model.WireGuardServer;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import bennyhils.inc.tgbot.vpn.WireGuardService;
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

@Slf4j
public class EnrichClients implements Action {


    private final Properties properties;

    private final WireGuardService wireGuardService = new WireGuardService();

    private final OutlineService outlineService = new OutlineService();

    public EnrichClients(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {

        return new SendMessage(
                update.getMessage().getChatId().toString(),
                "Введите '+' для обогащения всех клиентов  или введите tgId клиента:"
        );
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        String allOrTgId = update.getMessage() != null ? update.getMessage().getText() : null;
        if (allOrTgId != null) {

            Map<String, WireGuardServer> allWireGuardClientsToServerMap = wireGuardService.getServersClients(properties);
            List<WireGuardClient> allWGWireGuardClients = new ArrayList<>();
            for (String server : allWireGuardClientsToServerMap.keySet()) {
                allWGWireGuardClients.addAll(allWireGuardClientsToServerMap.get(server).getWireGuardClients());
            }

            Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                    properties);


            if (allOrTgId.equals(properties.getProperty("tg.admin.yes.word"))) {
                for (String s : outlineServersWithClientsMap.keySet()) {
                    for (OutlineClient c : outlineServersWithClientsMap.get(s).getClients()) {
                        WireGuardClient wireGuardClient = allWGWireGuardClients
                                .stream()
                                .filter(w -> w.getTgId().equals(c.getName()))
                                .findFirst()
                                .orElse(null);
                        if (wireGuardClient == null) {
                            return new SendMessage(
                                    update.getMessage().getChatId().toString(),
                                    "Не удалось найти клиента WireGuard"
                            );
                        }
                        enrichClient(wireGuardClient, c, s);
                    }
                }

                return new SendMessage(
                        update.getMessage().getFrom().getId().toString(),
                        "Обогащены все клиенты"
                );

            } else {
                WireGuardClient wireGuardClient = allWGWireGuardClients
                        .stream()
                        .filter(w -> w.getTgId().equals(allOrTgId))
                        .findFirst()
                        .orElse(null);
                if (wireGuardClient == null) {
                    return new SendMessage(
                            update.getMessage().getChatId().toString(),
                            "Не удалось найти клиента WireGuard"
                    );
                }
                for (String s : outlineServersWithClientsMap.keySet()) {
                    for (OutlineClient c : outlineServersWithClientsMap.get(s).getClients()) {
                        if (c.getName().equals(allOrTgId)) {
                            enrichClient(wireGuardClient, c, s);
                            break;
                        }
                    }
                }

            }

            return new SendMessage(
                    update.getMessage().getFrom().getId().toString(),
                    "Обогащен один клиент с tgId: '" + allOrTgId + "'"
            );

        } else {

            return new SendMessage(
                    update.getMessage().getFrom().getId().toString(),
                    "Не указаны параметры для обогащения!"
            );
        }
    }

    private void enrichClient(
            WireGuardClient wireGuardClient,
            OutlineClient outlineClient,
            String outlineServer
    ) {

        outlineService.updateTgData(
                outlineServer,
                outlineClient.getId().toString(),
                wireGuardClient.getTgLogin(),
                wireGuardClient.getTgFirst(),
                wireGuardClient.getTgLast()
        );

        outlineClient = outlineService.getClientByTgId(outlineService.getOutlineServersWithClientsMap(
                properties), wireGuardClient.getTgId()).get(outlineService.getOutlineServersWithClientsMap(
                properties).keySet().stream().findFirst().orElse(null));

        log.info(
                "У клиента '{}' обновлены данные Телеграм на: '{}', '{}', '{}'",
                outlineClient.getTgLogin().equals("null") ? outlineClient.getName() : outlineClient.getTgLogin(),
                outlineClient.getTgLogin(),
                outlineClient.getTgFirst(),
                outlineClient.getTgLast()
        );


        Instant newPaidBefore = wireGuardClient.getPaidBefore().isBefore(Instant.now()) ?
                Instant
                        .now()
                        .plus(
                                Long.parseLong(properties.getProperty("free.days.period")),
                                ChronoUnit.DAYS
                        ) :
                wireGuardClient
                        .getPaidBefore()
                        .plus(
                                Long.parseLong(properties.getProperty("free.days.period")),
                                ChronoUnit.DAYS
                        );
        outlineService.updatePaidBefore(
                outlineServer,
                newPaidBefore,
                outlineClient.getId().toString()
        );
        log.info(
                "У клиента '{}' обновлена оплата до '{}'",
                outlineClient.getTgLogin().equals("null") ? outlineClient.getName() : outlineClient.getTgLogin(),
                DataTimeUtil.getNovosibirskTimeFromInstant(newPaidBefore)
        );
        Instant correctUpdatedDate = wireGuardClient
                .getCreatedAt()
                // Фиксинг бага WireGuard
                .minus(2L, ChronoUnit.DAYS);
        outlineService.updateCreatedAtAndUpdatedAt(
                outlineServer,
                outlineClient.getId().toString(),
                correctUpdatedDate,
                wireGuardClient.getUpdatedAt()
        );
        log.info(
                "У клиента '{}' обновлены даты создания и обновления на: '{}', '{}'",
                outlineClient.getTgLogin().equals("null") ? outlineClient.getName() : outlineClient.getTgLogin(),
                DataTimeUtil.getNovosibirskTimeFromInstant(correctUpdatedDate),
                DataTimeUtil.getNovosibirskTimeFromInstant(wireGuardClient.getUpdatedAt())
        );

        outlineClient = outlineService.getClientByTgId(outlineService.getOutlineServersWithClientsMap(
                properties), wireGuardClient.getTgId()).get(outlineService.getOutlineServersWithClientsMap(
                properties).keySet().stream().findFirst().orElse(null));
        Map<String, Long> dataUsage = outlineService.getDataUsage(properties);

        log.info(
                "Теперь клиент выглядит так: '\n{}'",
                outlineClient.toStringForFileWithDataUsage(dataUsage.get(outlineClient.getName()) != null ?
                        dataUsage.get(outlineClient.getName()) / 1000000 + " МБ" :
                        "0 МБ")
        );

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
    public List<PartialBotApiMethod<Message>> sendPhoto(Update update) {

        return null;
    }
}
