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
public record Help(String supportName) implements Action {

    public final static String INITIAL_HELP_RESP = """
            Если у вас не работает VPN, то попробуйте выполнить три простых шага:
            
            1. Включите, выключите авиарежим, попробуйте подключиться снова и если не поможет ->
            2. Включите авиарежим, перезагрузите приложение (свайпом вверх), выключите авиарежим и попробуйте подключиться снова и если не поможет ->
            3. Перезагрузите телефон и попробуйте подключиться снова.
            
            """;

    public final static String YES = "да";

    public final static String NO = "нет";

    @Override
    public List<BotApiMethod<?>> handle(Update update) {

        return List.of(new SendMessage(
                update.getMessage().getChatId().toString(),
                INITIAL_HELP_RESP +
                        "Если инструкция не помогла или у вас другой вопрос — обратитесь в нашу дружелюбную поддержку " + supportName
        ));
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().toLowerCase();

            return switch (text) {
                case YES -> List.of(new SendMessage(
                        update.getMessage().getChatId().toString(),
                        """
                                Спасибо, что выбираете нас!
                                
                                Рекомендуем использовать программу лояльности.
                                Если кратко — вы приглашаете друзей и пользуетесь VPN бесплатно.
                                                                   \s
                                Подробности в /loyalty"""
                ));
                case NO -> List.of(new SendMessage(
                        update.getMessage().getChatId().toString(),
                        "Если инструкция не помогла или у вас другой вопрос — обратитесь в нашу дружелюбную поддержку " + supportName
                ));
                default -> List.of(new SendMessage(
                        update.getMessage().getChatId().toString(),
                        INITIAL_HELP_RESP
                ));
            };
        }

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
    public Map<Long, List<PartialBotApiMethod<Message>>> sendMassMessages(Update update) {

        return null;
    }
}
