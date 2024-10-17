package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FindClient implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public FindClient(Properties properties) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {

        return List.of(new SendMessage(
                update.getMessage().getChatId().toString(),
                "Введите минимум 3 символа для поиска клиента:"
        ));
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {
        String text = update.getMessage().getText();


        if (text.length() <= 2) {
            return List.of(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Повторите запрос минимум с 3 символами для поиска!\n\n/f"
            ));
        }
        List<OutlineClient> allOutlineClients = outlineService.getAllServersClients(properties);
        Map<String, Long> dataUsage = outlineService.getDataUsage(properties);
        Set<OutlineClient> outlineClient = allOutlineClients
                .stream()
                .filter(c -> c.getName() != null &&
                             c.getTgLogin() != null &&
                             c.getTgFirst() != null &&
                             c.getTgLast() != null)
                .filter(
                        c -> (c.getName().equals(text)) ||
                             c.getName().contains(text) ||
                             c.getTgLogin().equalsIgnoreCase(text) ||
                             c.getTgLogin().equalsIgnoreCase(text.replace("@", "")) ||
                             c.getTgLogin().toUpperCase().contains(text.toUpperCase()) ||
                             c.getTgFirst().equalsIgnoreCase(text) ||
                             c.getTgFirst().toUpperCase().contains(text.toUpperCase()) ||
                             c.getTgLast().equalsIgnoreCase(text) ||
                             c.getTgLast().toUpperCase().contains(text.toUpperCase()))
                .collect(Collectors.toSet());


        String clients = "Нашлись такие клиенты:\n\n" +
                         outlineClient
                                 .stream()
                                 .map(oc -> oc
                                         .toStringForFileWithDataUsage(
                                                 dataUsage.get(oc.getName()) != null ?
                                                         dataUsage.get(oc.getName()) / 1000000 + " МБ" :
                                                         "0 МБ"))
                                 .collect(Collectors.joining("\n\n"));
        String s = "Нашлось слишком много клиентов и они не помещаются в 1 сообщение. Уточните параметры поиска и повторите!\n\n/f\n\n";
        SendMessage sendMessage = new SendMessage(
                update.getMessage().getChatId().toString(),
                clients.length() >
                3072 ? s : clients
        );

        sendMessage.enableHtml(true);

        return outlineClient.isEmpty() ?
                List.of(new SendMessage(
                        update.getMessage().getChatId().toString(),
                        "Никого не нашлось. \n\nИскали по: " + text
                )) : List.of(sendMessage);
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
