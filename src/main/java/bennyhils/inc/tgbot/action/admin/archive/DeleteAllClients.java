package bennyhils.inc.tgbot.action.admin.archive;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
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

@Slf4j
public class DeleteAllClients implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public DeleteAllClients(Properties properties) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                properties);
        int d = 0;
        for (String s : outlineServersWithClientsMap.keySet()) {
            for (OutlineClient ignored : outlineServersWithClientsMap.get(s).getClients()) {
                d++;
            }
        }
        String msg = "Будет удалено БЕЗВОЗВРАТНО '" + d + "' клиентов. Вы уверены?";

        return List.of(new SendMessage(update.getMessage().getChatId().toString(), msg));
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {

        if (update.getMessage().getText().equals(properties.getProperty("tg.admin.yes.word"))) {
            Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                    properties);

            int d = 0;
            for (String s : outlineServersWithClientsMap.keySet()) {
                for (OutlineClient c : outlineServersWithClientsMap.get(s).getClients()) {
                    outlineService.deleteClient(s, c.getId().toString());
                    log.info("Удален клиент с id: {} и tgId: {}", c.getId(), c.getName());
                    d++;
                }
            }
            String msg = "Удалено клиентов: " + d;

            return List.of(new SendMessage(update.getMessage().getChatId().toString(), msg));
        } else {

            return List.of(new SendMessage(update.getMessage().getChatId().toString(), "Удаление всех клиентов отменено"));
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
}
