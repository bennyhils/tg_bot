package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class RecreateClient implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public RecreateClient(Properties properties) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        SendMessage message = new SendMessage(
                update.getMessage().getChatId().toString(),
                """
                        Будет удален старый и создан новый ключ клиента на новом порту. Пользователю необходимо перенастроить приложение Outline: удалить старый ключ и вставить новый из /instruction\s
                        
                        Введите логин или Id пользователя, срок бесплатного продления после пересоздания в днях через пробел.
                        
                        Например: <code>bennyhils 1</code>"""
        );
        message.enableHtml(true);

        return List.of(message);
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {
        String message = update.getMessage().getText();
        String[] parts = message.split("[,\\s]+");

        if (parts.length != 2) {
            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Введена неправильная команда для пересоздания пользователя!"
            ));
        }

        long freeDays;

        try {
            freeDays = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Введено неправильное количество дней бесплатного продления после пересоздания пользователя!"
            ));
        }

        Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                properties);

        var clientsForRecreate = getClientsForRecreate(parts[0].replace("@", ""), outlineServersWithClientsMap);

        if (clientsForRecreate.size() != 1) {
            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Пользователь с tgId или логином " + parts[0] + " не найден или найдено несколько пользователей для смены порта!!"
            ));
        }

        var recreatingClient = clientsForRecreate.get(0);
        Map<String, OutlineClient> clientByTgId = outlineService.getClientByTgId(
                outlineServersWithClientsMap,
                recreatingClient.getName()
        );

        Integer clientPort = recreatingClient.getPort();
        String server = clientByTgId.keySet().stream().findFirst().orElse(null);
        Integer serverPortForNewClient = outlineService
                .getServerNative(server)
                .getPortForNewAccessKeys();

        if (clientPort.equals(serverPortForNewClient)) {
            //new port from 1 to 65 535
            int newPort = (int) ((Math.random() * (65535 - 2001)) + 2001);
            outlineService.setPortForNewAccessKeys(server, newPort);
        }
        outlineService.deleteClient(
                server,
                recreatingClient.getId().toString()
        );

        OutlineClient updatedKeyClient = outlineService.createClient(
                server,
                0,
                recreatingClient.getName(),
                recreatingClient.getTgLogin(),
                recreatingClient.getTgFirst(),
                recreatingClient.getTgLast()
        );
        outlineService.updatePaidBefore(
                server,
                recreatingClient.getPaidBefore().plus(Math.abs(freeDays), ChronoUnit.DAYS),
                updatedKeyClient.getId().toString()
        );

        SendMessage sendMessage = new SendMessage(
                update.getMessage().getChatId().toString(),
                "Обновили клиенту " +
                        updatedKeyClient.getName() +
                        " ключ \nс <code>" +
                        recreatingClient.getAccessUrl() +
                        "</code> \nна <code>" +
                        updatedKeyClient.getAccessUrl() +
                        "</code>. \n\n" +
                        "Также обновили дату оплаты \nс " +
                        DataTimeUtil.getNovosibirskTimeFromInstant(recreatingClient.getPaidBefore()) +
                        " \nна " +
                        DataTimeUtil.getNovosibirskTimeFromInstant(recreatingClient
                                .getPaidBefore()
                                .plus(Math.abs(freeDays), ChronoUnit.DAYS))
        );
        sendMessage.enableHtml(true);

        return List.of(sendMessage);
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

    private List<OutlineClient> getClientsForRecreate(String clientsForUpdate, Map<String, OutlineServer> outlineServersWithClientsMap) {
        var allClients = new ArrayList<OutlineClient>();
        for (var client : outlineServersWithClientsMap.values().stream().map(OutlineServer::getClients).toList()) {
            allClients.addAll(client);
        }
        return allClients
                .stream()
                .filter(outlineClient -> outlineClient.getName().equals(clientsForUpdate) ||
                        outlineClient.getTgLogin().equals(clientsForUpdate)).toList();

    }
}
