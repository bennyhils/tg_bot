package bennyhils.inc.tgbot.action;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Slf4j
public class Buy implements Action {

    public final static String ONE_MONTH = "1";
    public final static String THREE_MONTHS = "3";
    public final static String SIX_MONTHS = "6";

    //цена в копейках
    public final static Integer ONE_MONTH_PRICE = 11900;
    public final static Integer THREE_MONTHS_PRICE = 34900;
    public final static Integer SIX_MONTHS_PRICE = 64900;

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public Buy(
            Properties properties
    ) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {

        var msg = update.getMessage();
        var chatId = msg.getChatId().toString();
        var tgId = msg.getFrom().getId().toString();

        Map<String, OutlineServer> initialServersClientsMap = outlineService.getOutlineServersWithClientsMap(properties);

        Map<String, OutlineClient> initialClientServerMap = outlineService.getClientByTgId(
                initialServersClientsMap,
                tgId
        );

        OutlineClient existingOutlineClient = initialClientServerMap.get(initialClientServerMap
                .keySet()
                .stream()
                .findFirst()
                .isPresent() ?
                initialClientServerMap.keySet().stream().findFirst().get() : null);

        if (existingOutlineClient != null) {

            Instant now = Instant.now();
            Instant paidBefore = existingOutlineClient.getPaidBefore();

            if (now.isAfter(paidBefore)) {
                return sendInlineKeyBoardMessage(
                        Long.parseLong(chatId),
                        "У вас был наш VPN, но срок его оплаты закончился в " +
                        DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) + "." +
                        "\n" +
                        "\n"
                );
            } else {
                return sendInlineKeyBoardMessage(
                        Long.parseLong(chatId),
                        "У вас есть доступ до " +
                        DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) +
                        ".\n" +
                        "" +
                        "\n" +
                        "Вы можете продлить доступ заранее, не дожидаясь отключения." +
                        "\n" +
                        "\n"
                );
            }
        } else {

            OutlineClient createdOutlineClient = outlineService.createClient(
                    outlineService.findServerForClientCreation(properties),
                    Integer.parseInt(properties.getProperty("free.days.period")),
                    msg.getFrom().getId().toString(),
                    msg.getFrom().getUserName(),
                    msg.getFrom().getFirstName(),
                    msg.getFrom().getLastName()
            );

            Instant paidBefore = createdOutlineClient.getPaidBefore();

            return sendInlineKeyBoardMessage(
                    Long.parseLong(chatId),
                    "Поздравляем!" +
                    "\n" +
                    "\n" +
                    "Мы создали вам клиента с бесплатным доступом на " +
                    properties.getProperty("free.days.period") +
                    " дн. до " +
                    DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) +
                    "." +
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
            data = ONE_MONTH;
            Integer totalAmount = update.getMessage().getSuccessfulPayment().getTotalAmount();

            if (totalAmount.equals(THREE_MONTHS_PRICE)) {
                data = THREE_MONTHS;
            }
            if (totalAmount.equals(SIX_MONTHS_PRICE)) {
                data = SIX_MONTHS;
            }
        } else if (update.getCallbackQuery() == null && update.getPreCheckoutQuery() != null) {
            tgId = update.getPreCheckoutQuery().getFrom().getId().toString();
            chatId = update.getPreCheckoutQuery().getFrom().getId().toString();
            data = ONE_MONTH;
            Integer totalAmount = update.getPreCheckoutQuery().getTotalAmount();

            if (totalAmount.equals(THREE_MONTHS_PRICE)) {
                data = THREE_MONTHS;
            }
            if (totalAmount.equals(SIX_MONTHS_PRICE)) {
                data = SIX_MONTHS;
            }
        } else {
            if (update.getCallbackQuery() == null) {
                return null;
            }
            tgId = update.getCallbackQuery().getFrom().getId().toString();
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            data = update.getCallbackQuery().getData();
        }
        Map<String, OutlineClient> serverClient =
                outlineService.getClientByTgId(outlineService.getOutlineServersWithClientsMap(properties), tgId);
        if (serverClient == null) {
            log.error("Ошибка получения клиента по tgId '{}'", tgId);
            return null;
        }
        String clientServer = serverClient.keySet().stream().findFirst().isPresent() ?
                serverClient.keySet().stream().findFirst().get() : null;
        OutlineClient outlineClient = serverClient.get(clientServer);
        Instant paidBefore = outlineClient.getPaidBefore();
        Instant now = Instant.now();
        if (paidBefore.isBefore(now)) {
            paidBefore = LocalDateTime
                    .ofInstant(now, ZoneOffset.UTC)
                    .plusMonths(Integer.parseInt(data)).toInstant(ZoneOffset.UTC);
        } else {
            paidBefore = LocalDateTime
                    .ofInstant(paidBefore, ZoneOffset.UTC)
                    .plusMonths(Integer.parseInt(data)).toInstant(ZoneOffset.UTC);
        }

        outlineService.updatePaidBefore(
                clientServer,
                paidBefore,
                outlineClient.getId().toString()
        );
        outlineService.enableClient(clientServer, outlineClient.getId().toString());
        var text = "Доступ оплачен до " +
                   DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) + "." +
                   "" +
                   "\n" +
                   "\n" +
                   "Если вам необходим чек, обратитесь в нашу дружелюбную поддержку " +
                   properties.getProperty("tg.support.user.name");

        return new SendMessage(chatId, text);
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {

        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {

        return null;
    }

    private SendMessage sendInlineKeyBoardMessage(long chatId, String altText) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton one = InlineKeyboardButton.builder()
                                                       .text(ONE_MONTH +
                                                             " мес.  — " +
                                                             Buy.ONE_MONTH_PRICE / 100 +
                                                             "₽")
                                                       .callbackData(ONE_MONTH)
                                                       .build();

        InlineKeyboardButton three = InlineKeyboardButton.builder()
                                                         .text(THREE_MONTHS +
                                                               " мес.  — " +
                                                               Buy.THREE_MONTHS_PRICE / 100 +
                                                               "₽")
                                                         .callbackData(THREE_MONTHS)
                                                         .build();

        List<InlineKeyboardButton> keyboardButtonsRowOne = new ArrayList<>();
        keyboardButtonsRowOne.add(one);
        keyboardButtonsRowOne.add(three);

        List<InlineKeyboardButton> keyboardButtonsRowTwo = new ArrayList<>();
        InlineKeyboardButton six = InlineKeyboardButton
                .builder()
                .text(SIX_MONTHS + " мес.  — " + Buy.SIX_MONTHS_PRICE / 100 + "₽")
                .callbackData(SIX_MONTHS)
                .build();
        keyboardButtonsRowTwo.add(six);

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
