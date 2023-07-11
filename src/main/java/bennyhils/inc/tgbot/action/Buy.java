package bennyhils.inc.tgbot.action;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import bennyhils.inc.tgbot.model.Client;
import bennyhils.inc.tgbot.model.Server;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.wireguard.WireGuardClient;
import bennyhils.inc.tgbot.wireguard.WireGuardService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Slf4j
public class Buy implements Action {

    public final static String ONE = "1";
    public final static String THREE = "3";
    public final static String SIX = "6";
    public final static String TWELVE = "12";

    //цена в копейках
    public final static Integer ONE_PRICE = 9900;
    public final static Integer THREE_PRICE = 27900;
    public final static Integer SIX_PRICE = 52900;
    public final static Integer TWELVE_PRICE = 99900;

    private final Map<String, Server> serversClients;
    private final Properties config;

    private final WireGuardClient wireGuardClient = new WireGuardClient();
    private final WireGuardService wireGuardService = new WireGuardService();

    public Buy(Map<String, Server> serversClients, Properties config) {
        this.serversClients = serversClients;
        this.config = config;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {

        var msg = update.getMessage();
        var chatId = msg.getChatId().toString();
        var tgId = msg.getFrom().getId().toString();

        Map<String, Server> serversClients = wireGuardService.getServersClients(config);
        Map<String, Client> serverClient = wireGuardService.getClientByTgId(config, tgId);

        Client existClient;

        if (serverClient == null) {
            existClient = null;
        } else {
            existClient = serverClient.get(serverClient.keySet().stream().findFirst().isPresent() ?
                    serverClient.keySet().stream().findFirst().get() : null);
        }

        if (existClient != null) {

            LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
            LocalDateTime paidBefore = existClient.getPaidBefore();

            if (now.isAfter(paidBefore)) {
                return sendInlineKeyBoardMessage(
                        Long.parseLong(chatId),
                        "У вас был наш VPN, но срок его оплаты закончился в " +
                        paidBefore.format(DataTimeUtil.DATE_TIME_FORMATTER) + "." +
                        "\n" +
                        "\n"
                );
            } else {
                return sendInlineKeyBoardMessage(
                        Long.parseLong(chatId),
                        "У вас есть доступ до " +
                        paidBefore.format(DataTimeUtil.DATE_TIME_FORMATTER) +
                        ".\n" +
                        "" +
                        "\n" +
                        "Вы можете продлить доступ заранее, не дожидаясь отключения." +
                        "\n" +
                        "\n"
                );
            }
        } else {
            String serverWithMinClientCount = serversClients.keySet().stream().findFirst().orElseThrow();
            for (String serverIp : serversClients.keySet()) {
                if (serversClients.get(serverIp).getClientsCount() <
                    serversClients.get(serverWithMinClientCount).getClientsCount()) {
                    // Сервер, где еще меньше клиентов
                    serverWithMinClientCount = serverIp;
                }
            }
            wireGuardClient.createClient(
                    msg.getFrom(),
                    serverWithMinClientCount,
                    serversClients.get(serverWithMinClientCount).getCookie()
            );

            serverClient = wireGuardService.getClientByTgId(config, tgId);

            if (serverClient == null) {
                log.error("Ошибка получения клиента по tgId '{}'", tgId);
                return null;
            }

            Client createdClient = serverClient.get(serverClient.keySet().stream().findFirst().get());

            LocalDateTime paidBefore = createdClient.getPaidBefore();

            return sendInlineKeyBoardMessage(
                    Long.parseLong(chatId),
                    "Поздравляем!" +
                    "\n" +
                    "\n" +
                    "Мы создали вам клиента с бесплатным доступом на 2 дня до " +
                    paidBefore.format(DataTimeUtil.DATE_TIME_FORMATTER) + "." +
                    "\n" +
                    "\n" +
                    "VPN уже работает, чтобы подключить его нажмите /instruction" +
                    "\n" +
                    "\n" +
                    "Вы можете продлить доступ заранее, не дожидаясь окончания тестового периода." +
                    "\n" +
                    "\n"
            );
        }
    }

    @Override
    public BotApiMethod<?> callback(Update update) {

        String chatId;
        String data;
        String tgId;

        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            tgId = update.getMessage().getFrom().getId().toString();
            chatId = update.getMessage().getChatId().toString();
            data = ONE;
            Integer totalAmount = update.getMessage().getSuccessfulPayment().getTotalAmount();

            if (totalAmount.equals(THREE_PRICE)) {
                data = THREE;
            }
            if (totalAmount.equals(SIX_PRICE)) {
                data = SIX;
            }
            if (totalAmount.equals(TWELVE_PRICE)) {
                data = TWELVE;
            }
        } else if (update.getCallbackQuery() == null) {
            tgId = update.getPreCheckoutQuery().getFrom().getId().toString();
            chatId = update.getPreCheckoutQuery().getFrom().getId().toString();
            data = ONE;
            Integer totalAmount = update.getPreCheckoutQuery().getTotalAmount();

            if (totalAmount.equals(THREE_PRICE)) {
                data = THREE;
            }
            if (totalAmount.equals(SIX_PRICE)) {
                data = SIX;
            }
            if (totalAmount.equals(TWELVE_PRICE)) {
                data = TWELVE;
            }

        } else {
            tgId = update.getCallbackQuery().getFrom().getId().toString();
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            data = update.getCallbackQuery().getData();
        }

        Map<String, Server> serversClients = wireGuardService.getServersClients(config);
        Map<String, Client> serverClient = wireGuardService.getClientByTgId(config, tgId);

        if (serverClient == null) {
            log.error("Ошибка получения клиента по tgId '{}'", tgId);
            return null;
        }

        String clientServer = serverClient.keySet().stream().findFirst().isPresent() ?
                serverClient.keySet().stream().findFirst().get() : null;

        Client clientById = serverClient.get(clientServer);

        LocalDateTime paidBefore = clientById.getPaidBefore();

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (paidBefore.isBefore(now)) {
            paidBefore = now.plusMonths(Integer.parseInt(data));
        } else {
            paidBefore = paidBefore.plusMonths(Integer.parseInt(data));
        }

        wireGuardClient.updatePaidBefore(
                clientById.getId(),
                paidBefore.toString(),
                clientServer,
                serversClients.get(clientServer).getCookie()
        );

        wireGuardClient.enableClient(
                clientById.getId(),
                clientServer,
                serversClients.get(clientServer).getCookie()
        );

        var text = "Доступ оплачен до " +
                   paidBefore.format(DataTimeUtil.DATE_TIME_FORMATTER) + "." +
                   "" +
                   "\n" +
                   "\n" +
                   "Если вам необходим чек, обратитесь в нашу дружелюбную поддержку " +
                   config.getProperty("tg.support.user.name");

        return new SendMessage(chatId, text);
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {
        String tgId;

        if (update.getCallbackQuery() != null) {
            tgId = update.getCallbackQuery().getFrom().getId().toString();
        } else {
            tgId = update.getMessage().getFrom().getId().toString();
        }

        Map<String, Client> serverClient = wireGuardService.getClientByTgId(config, tgId);

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

        SendDocument sendDocument = new SendDocument(
                tgId,
                new InputFile(
                        new ByteArrayInputStream(config.getBytes()),
                        update.getCallbackQuery() == null ?
                                tgId :
                                update.getCallbackQuery()
                                      .getFrom()
                                      .getId() + ".conf"
                )
        );
        sendDocument.setCaption("Config");

        return sendDocument;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {
        return null;
    }

    private SendMessage sendInlineKeyBoardMessage(long chatId, String altText) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton one = InlineKeyboardButton.builder()
                                                       .text(ONE + " мес.  — " + Buy.ONE_PRICE / 100 + "₽")
                                                       .callbackData(ONE)
                                                       .build();

        InlineKeyboardButton three = InlineKeyboardButton.builder()
                                                         .text(THREE + " мес.  — " + Buy.THREE_PRICE / 100 + "₽")
                                                         .callbackData(THREE)
                                                         .build();

        List<InlineKeyboardButton> keyboardButtonsRowOne = new ArrayList<>();
        keyboardButtonsRowOne.add(one);
        keyboardButtonsRowOne.add(three);

        List<InlineKeyboardButton> keyboardButtonsRowTwo = new ArrayList<>();
        InlineKeyboardButton six = InlineKeyboardButton
                .builder()
                .text(SIX + " мес.  — " + Buy.SIX_PRICE / 100 + "₽")
                .callbackData(SIX)
                .build();
        InlineKeyboardButton twelve = InlineKeyboardButton
                .builder()
                .text(TWELVE + " мес. — " + Buy.TWELVE_PRICE / 100 + "₽")
                .callbackData(TWELVE)
                .build();
        keyboardButtonsRowTwo.add(six);
        keyboardButtonsRowTwo.add(twelve);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRowOne);
        rowList.add(keyboardButtonsRowTwo);

        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        if (altText == null) {
            sendMessage.setText("На сколько месяцев вы покупаете VPN?");
        } else {
            sendMessage.setText(altText + "На сколько месяцев вы покупаете VPN?");
        }
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }
}
