package bennyhils.inc.tgbot.action;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

public interface Action {

    List<BotApiMethod<?>> handle(Update update);

    List<BotApiMethod<?>> callback(Update update);

    PartialBotApiMethod<Message> sendDocument(Update update);

    PartialBotApiMethod<Message> sendVideo(Update update);

    Map<Long, List<PartialBotApiMethod<Message>>> sendMassMessages(Update update);
}
