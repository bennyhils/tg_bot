package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.vpn.OutlineService;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GetClients implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public GetClients(Properties properties) {
        this.properties = properties;
    }


    @Override
    public List<BotApiMethod<?>> handle(Update update) {

        List<OutlineClient> allOutlineClients = outlineService.getAllServersClients(properties);
        Map<String, OutlineServer> outlineServerConfigs = outlineService.getOutlineServersWithClientsMap(properties);

        String msg = """
                %s серв.
                                
                %s клиентов, %s включенных
                                
                """.formatted(
                outlineServerConfigs.keySet().size(),
                allOutlineClients.size(),
                allOutlineClients.stream().filter(outlineClient -> outlineClient.getDataLimit() == null).collect(
                        Collectors.toSet()).size()
        );

        StringBuilder result = new StringBuilder();

        int i = 1;

        for (String s : outlineServerConfigs.keySet()) {
            Set<OutlineClient> noLimit = outlineServerConfigs
                    .get(s)
                    .getClients()
                    .stream()
                    .filter(outlineClient -> outlineClient.getDataLimit() == null)
                    .collect(Collectors.toSet());
            result
                    .append("    ")
                    .append(i)
                    .append(" сервер: ")
                    .append(outlineServerConfigs.get(s).getClientsCount())
                    .append(" клиентов, ")
                    .append(noLimit.size())
                    .append(" включенных")
                    .append("\n");
            i++;
        }

        result.append("\nВыслать файл со всеми клиентами?");

        return List.of(new SendMessage(update.getMessage().getChatId().toString(), msg + result));
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {
        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {

        if (update.getMessage().getText().equals(properties.getProperty("tg.admin.yes.word"))) {
            Map<String, OutlineServer> outlineServerConfigs = outlineService.getOutlineServersWithClientsMap(properties);
            Map<String, Long> dataUsage = outlineService.getDataUsage(properties);
            InputStream targetStream;

            StringBuilder result = new StringBuilder();

            int i = 1;
            for (String s : outlineServerConfigs.keySet()) {
                result.append(i).append(" Сервер: ").append(s).append(" \n");
                result.append(
                        "    Id, TgId, Оплатил до, TgLogin, TgFirst, TgLast, ВКЛ/ВЫКЛ, Использовал трафика, Ключ \n");
                i++;
                for (OutlineClient c : outlineServerConfigs.get(s).getClients()) {

                    result
                            .append("\n    ")
                            .append(c.getId())
                            .append(", ")
                            .append(c.getName())
                            .append(", ")
                            .append(DataTimeUtil.getNovosibirskTimeFromInstant(c.getPaidBefore()))
                            .append(", ")
                            .append(c.getTgLogin())
                            .append(", ")
                            .append(c.getTgFirst())
                            .append(", ")
                            .append(c.getTgLast())
                            .append(", ")
                            .append(c.getDataLimit() != null ? c.getDataLimit().getBytes() / 1024 / 1024 +
                                    " МБ / ВЫКЛ" : "ВКЛ")
                            .append(", ")
                            .append(dataUsage.get(c.getName()) != null ?
                                    dataUsage.get(c.getName()) / 1000000 + " МБ" :
                                    "0 МБ")
                            .append(", ")
                            .append(c.getAccessUrl())
                            .append("\n")

                    ;
                }
                result.append("\n");
            }

            targetStream = new ByteArrayInputStream(result.toString().getBytes());

            SendDocument sendDocument = new SendDocument(
                    update.getMessage().getChatId().toString(),
                    new InputFile(targetStream, DataTimeUtil.getNovosibirskTimeFromInstant(Instant.now()) + ".txt")
            );
            sendDocument.setCaption("Клиенты");

            return sendDocument;
        } else {

            return null;
        }
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
