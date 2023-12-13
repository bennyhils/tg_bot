package bennyhils.inc.tgbot.action;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class Info implements Action {

    private final Properties properties;


    OutlineService outlineService = new OutlineService();

    public Info(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();
        var chatId = msg.getChatId().toString();

        Map<String, OutlineClient> outlineClientMap = outlineService.getClientByTgId(outlineService.getOutlineServersWithClientsMap(
                properties), tgId);

        OutlineClient existingOutlineClient = null;

        if (outlineClientMap != null) {
            existingOutlineClient = outlineClientMap.get(outlineClientMap.keySet().stream().findFirst().isPresent() ?
                    outlineClientMap.keySet().stream().findFirst().get() : null);
        }

        if (existingOutlineClient == null) {
            return new SendMessage(chatId, """
                    У вас нет нашего VPN.

                    Чтобы получить его бесплатно на %s дн., нажмите /buy
                    """.formatted(properties.getProperty("free.days.period")));

        } else {

            Instant now = Instant.now();

            Instant paidBefore = existingOutlineClient.getPaidBefore();

            if (now.isAfter(paidBefore)) {
                return new SendMessage(
                        chatId,
                        """
                                У вас был наш VPN, но срок его действия закончился в %s.
                                       
                                       
                                Чтобы оплатить его нажмите /buy""".formatted(DataTimeUtil.getNovosibirskTimeFromInstant(
                                paidBefore))
                );
            } else {
                return new SendMessage(chatId, """
                        У вас есть доступ до %s.

                        Вы можете продлить доступ заранее, не дожидаясь отключения, для этого нажмите /buy"""
                        .formatted(DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore)));
            }
        }
    }

    @Override
    public BotApiMethod<?> callback(Update update) {

        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(
            Update update
    ) {

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
