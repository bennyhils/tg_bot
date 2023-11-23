package bennyhils.inc.tgbot.action.admin.archive;

import bennyhils.inc.tgbot.action.Action;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;


@Slf4j
public class SendMigrationMessage implements Action {

    @Override
    public BotApiMethod<?> handle(Update update) {

        return null;
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
}
