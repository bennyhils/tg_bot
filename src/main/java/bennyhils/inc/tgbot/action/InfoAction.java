package bennyhils.inc.tgbot.action;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public record InfoAction(List<String> actions) implements Action {

    @Override
    public BotApiMethod<?> handle(Update update) {
        var msg = update.getMessage();
        var chatId = msg.getChatId().toString();
        var out = new StringBuilder();
        out.append("Команды:").append("\n");
        for (String action : actions) {
            out.append(action).append("\n");
        }
        return new SendMessage(chatId, out.toString());
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        return handle(update);
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {
        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {
        return null;
    }
}
