package bennyhils.inc.tgbot.action;

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
public class Loyalty implements Action {

    private final Properties properties;

    public Loyalty(
            Properties properties
    ) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {

        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();

        return List.of(new SendMessage(tgId,
                "Если ваш друг зарегистрируется по этой ссылке и оплатит подписку, вы получите бесплатный доступ к VPN на срок, равный его первой подписке (1, 3 или 6 месяцев) \n \n https://t.me/" + properties.getProperty("tg.username") + "?start=" + tgId));
    }


    @Override
    public List<BotApiMethod<?>> callback(Update update) {

        return null;
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
