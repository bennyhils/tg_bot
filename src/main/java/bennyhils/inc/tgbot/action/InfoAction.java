package bennyhils.inc.tgbot.action;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

@Slf4j
public record InfoAction(List<String> actions) implements Action {

    @Override
    public BotApiMethod<?> handle(Update update) {
        var msg = update.getMessage();
        var chatId = msg.getChatId().toString();
        var out = new StringBuilder();
        out.append("Для работы с ботом используйте следующие команды:").append("\n");
        for (String action : actions) {
            out.append(action).append("\n");
        }

        return new SendMessage(chatId, out.toString());
    }

    @Override
    public BotApiMethod<?> callback(Update update) {

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
