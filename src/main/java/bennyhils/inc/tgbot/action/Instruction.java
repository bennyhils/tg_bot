package bennyhils.inc.tgbot.action;

import bennyhils.inc.tgbot.model.Client;
import bennyhils.inc.tgbot.model.Server;
import bennyhils.inc.tgbot.wireguard.WireGuardClient;
import bennyhils.inc.tgbot.wireguard.WireGuardService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class Instruction implements Action {

    public final static String IOS = "IOS";
    public final static String ANDROID = "Android";

    private final Map<String, Server> serversClients;
    private final Properties properties;

    private final WireGuardClient wireGuardClient = new WireGuardClient();
    private final WireGuardService wireGuardService = new WireGuardService();

    public Instruction(Map<String, Server> serversClients, Properties properties) {
        this.serversClients = serversClients;
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        String tgId = update.getMessage().getFrom().getId().toString();
        Map<String, Client> serverClient = wireGuardService.getClientByTgId(properties, tgId);

        Client clientByTgId = null;

        if (serverClient != null) {
            clientByTgId = wireGuardService.getClientByTgId(properties, tgId).get(
                    serverClient.keySet().stream().findFirst().isPresent() ?
                            serverClient.keySet().stream().findFirst().get() : null);
            if (clientByTgId == null) {
                log.error("Ошибка получения клиента по tgId '{}'", tgId);
                return null;
            }
        }

        if (clientByTgId == null) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    """
                            У вас нету нашего VPN.

                            Чтобы получить его бесплатно на 2 дня, нажмите /buy"""
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
                    "https://apps.apple.com/ru/app/wireguard/id1441195209"
            );
            case (ANDROID) -> sendInstruction(
                    Long.parseLong(chatId),
                    "https://play.google.com/store/apps/details?id=com.wireguard.android"
            );
            default -> null;
        };
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(
            Update update
    ) {
        String tgId = update.getCallbackQuery().getFrom().getId().toString();

        Map<String, Client> serverClient = wireGuardService.getClientByTgId(properties, tgId);

        if (serverClient == null) {
            log.error("Ошибка получения клиента по tgId '{}'", tgId);
            return null;
        }

        String clientServer = serverClient.keySet().stream().findFirst().isPresent() ?
                serverClient.keySet().stream().findFirst().get() : null;

        Client clientById = serverClient.get(clientServer);

        String config = wireGuardClient
                .getPeerConfig(clientById.getId(), clientServer, this.serversClients.get(clientServer).getCookie())
                .body();

        InputStream targetStream = new ByteArrayInputStream(config.getBytes());

        SendDocument sendDocument = new SendDocument(
                update.getCallbackQuery().getMessage().getChatId().toString(),
                new InputFile(targetStream, update.getCallbackQuery().getFrom().getId() + ".conf")
        );
        sendDocument.setCaption("Файл для подключения");

        return sendDocument;
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

    private SendMessage sendInstruction(long chatId, String link) {
        SendMessage sendMessage = new SendMessage(
                String.valueOf(chatId),
                "Посмотрите небольшую видеоинструкцию, чтобы подключить VPN. " +
                "\n" +
                "\n" +
                "Но если в двух словах:" +
                "\n" +
                "1. Установите <a href=\"" + link + "\">приложение WireGuard</a>" +
                "\n" +
                "2. Откройте <b>Файл для подключения</b>, присланный ботом выше, в приложении WireGuard. Разрешите все доступы, которые просит приложение, это необходимо для работы"
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
