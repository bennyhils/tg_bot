package bennyhils.inc.tgbot.action;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface Action {

    BotApiMethod<?> handle(Update update);

    BotApiMethod<?> callback(Update update);

    PartialBotApiMethod<Message> sendDocument(Update update);

    PartialBotApiMethod<Message> sendVideo(Update update);

    List<PartialBotApiMethod<Message>> sendPhoto(Update update);
}
