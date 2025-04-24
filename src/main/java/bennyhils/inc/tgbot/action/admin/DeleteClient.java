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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class DeleteClient implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public DeleteClient(Properties properties) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        String msg = "Введите логин или Id пользователя, ключ которого хотите удалить, и подтверждающее слово через пробел.\n\nНапример: <code>bennyhils да</code>";
        SendMessage sendMessage = new SendMessage(update.getMessage().getChatId().toString(), msg);
        sendMessage.enableHtml(true);
        return List.of(sendMessage);
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {
        String message = update.getMessage().getText();
        String[] parts = message.split("[,\\s]+");
        if (parts.length != 2) {
            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Введена неправильная команда для удаления!"
            ));
        }
        if (!parts[1].equals(properties.getProperty("tg.admin.yes.word"))) {
            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Неправильное слово подтверждения!"
            ));
        } else {

            Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                    properties);

            var deletingOutlineClients = getClientForDelete(parts[0].replace("@", ""), outlineServersWithClientsMap);

            if (deletingOutlineClients.size() != 1 ) {
                return List.of(new SendMessage(
                        update.getMessage().getChatId().toString(),
                        "Пользователь с tgId или логином " + parts[0] + " не найден или найдено несколько пользователей для удаления!"
                ));
            }

            Map<String, OutlineClient> clientByTgId = outlineService.getClientByTgId(
                    outlineServersWithClientsMap,
                    deletingOutlineClients.get(0).getName()
            );

            String server = clientByTgId.keySet().stream().findFirst().orElse(null);
            if (server == null) {
                return List.of(new SendMessage(update.getMessage().getChatId().toString(), "Не найден сервер клиента"));
            }
            String clientId = clientByTgId.get(server).getId().toString();
            if (clientId.isEmpty()) {
                return List.of(new SendMessage(
                        update.getMessage().getChatId().toString(),
                        "Не найден клиент на сервере: " + server
                ));
            }
            outlineService.deleteClient(
                    server,
                    clientId
            );

            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Удален клиент " + parts[0] + " и у него было оплачено до " +
                            DataTimeUtil.getNovosibirskTimeFromInstant(clientByTgId.get(server).getPaidBefore())
            ));
        }
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

    private List<OutlineClient> getClientForDelete(String clientsForUpdate, Map<String, OutlineServer> outlineServersWithClientsMap) {
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
