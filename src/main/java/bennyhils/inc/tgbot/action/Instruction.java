package bennyhils.inc.tgbot.action;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class Instruction implements Action {

    public final static String IOS = "IOS";
    public final static String ANDROID = "Android";

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public Instruction(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        String tgId = update.getMessage().getFrom().getId().toString();

        Map<String, OutlineClient> outlineClientMap =
                outlineService.getClientByTgId(outlineService.getOutlineServersWithClientsMap(properties), tgId);

        OutlineClient outlineClient = outlineClientMap.get(outlineClientMap.keySet().stream().findFirst().orElse(null));

        if (outlineClient == null) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    """
                            У вас нет нашего VPN.

                            Чтобы получить его бесплатно на %s дн., нажмите /buy""".formatted(properties.getProperty(
                            "free.days.period"))
            );
        }
        var msg = update.getMessage();
        var chatId = msg.getChatId();

        return sendInlineKeyBoardMessage(chatId);
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        String chatId = String.valueOf(callbackQuery.getMessage().getChatId());

        return switch (data) {
            case (IOS) -> sendInstruction(
                    Long.parseLong(chatId),
                    "https://apps.apple.com/ru/app/outline-app/id1356177741",
                    true
            );
            case (ANDROID) -> sendInstruction(
                    Long.parseLong(chatId),
                    "https://play.google.com/store/apps/details?id=org.outline.android.client",
                    false
            );
            default -> null;
        };
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(
            Update update
    ) {

        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is;
        if (update.getCallbackQuery().getData().equals(ANDROID)) {
            is = classloader.getResourceAsStream("Android.mp4");
        } else {
            is = classloader.getResourceAsStream("IOS.mp4");
        }

        return new SendVideo(
                update.getCallbackQuery().getMessage().getChatId().toString(),
                new InputFile(is, "Instruction.mp4")
        );
    }

    private SendMessage sendInstruction(long chatId, String link, boolean needAppend) {
        Map<String, OutlineClient> outlineClientMap =
                outlineService.getClientByTgId(
                        outlineService.getOutlineServersWithClientsMap(properties),
                        String.valueOf(chatId)
                );

        String messageTemplate = """
                Посмотрите небольшую видеоинструкцию, чтобы подключить VPN.
                                
                Но если в двух словах:
                                
                1. Установите <a href="%s">приложение Outline</a>
                                
                2. Скопируйте ключ (просто нажав на него) и вставьте его в приложение Outline
                                
                <code>%s</code>
                
                """.formatted(
                link,
                outlineClientMap.get(outlineClientMap.keySet().stream().findFirst().orElse(null)).getAccessUrl()
        );

        String iosInstructionAppend = """                                                               
                3. Разрешите Outline добавить конфигурацию VPN
                
                """;

        if (needAppend) {
            messageTemplate = messageTemplate + iosInstructionAppend;
        }

        SendMessage sendMessage = new SendMessage(
                String.valueOf(chatId),
                messageTemplate
        );

        sendMessage.enableHtml(true);

        return sendMessage;
    }

    private SendMessage sendInlineKeyBoardMessage(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton ios = new InlineKeyboardButton();
        ios.setText(IOS);
        ios.setCallbackData(IOS);
        InlineKeyboardButton android = new InlineKeyboardButton();
        android.setText(ANDROID);
        android.setCallbackData(ANDROID);
        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(ios);
        keyboardButtonsRow.add(android);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow);
        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("У вас IOS или Android?");
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        return sendMessage;
    }
}
