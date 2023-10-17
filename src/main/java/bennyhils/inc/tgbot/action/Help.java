package bennyhils.inc.tgbot.action;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
public record Help(String supportName) implements Action {

    @Override
    public BotApiMethod<?> handle(Update update) {

        return new SendMessage(
                update.getMessage().getChatId().toString(),
                "При вопросах с подключением обратитесь в нашу дружелюбную поддержку " + supportName
        );
    }

    @Override
    public BotApiMethod<?> callback(Update update) {

        return new SendMessage(
                update.getMessage().getChatId().toString(),
                "При вопросах с подключением обратитесь в нашу дружелюбную поддержку " + supportName
        );
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
}
