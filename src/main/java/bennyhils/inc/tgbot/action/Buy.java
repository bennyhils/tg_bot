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
import bennyhils.inc.tgbot.model.Referral;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.util.FileEngine;
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

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public Buy(
            Properties properties
    ) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {

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

                return List.of(sendInlineKeyBoardMessage(
                        Long.parseLong(chatId),
                        "У вас был наш VPN, но срок его оплаты закончился в " +
                                DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) + "." +
                                "\n" +
                                "\n"
                ));
            } else {
                return List.of(sendInlineKeyBoardMessage(
                        Long.parseLong(chatId),
                        "У вас есть доступ до " +
                                DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) +
                                ".\n" +
                                "\n" +
                                "Вы можете продлить доступ заранее, не дожидаясь отключения." +
                                "\n" +
                                "\n"
                ));
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

            return List.of(sendInlineKeyBoardMessage(
                    Long.parseLong(chatId),
                    "Поздравляем!" +
                            "\n" +
                            "\n" +
                            "Мы создали вам ключ с бесплатным доступом на " +
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
            ));
        }
    }


    @Override
    public List<BotApiMethod<?>> callback(Update update) {

        String chatId;
        String data;
        String tgId;
        Integer totalAmount = 0;

        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            tgId = update.getMessage().getFrom().getId().toString();
            chatId = update.getMessage().getChatId().toString();
            data = properties.getProperty("month.one");
            totalAmount = update.getMessage().getSuccessfulPayment().getTotalAmount();

            if (totalAmount.equals(Integer.valueOf(properties.getProperty("price.three.month")))) {
                data = properties.getProperty("month.three");
            }
            if (totalAmount.equals(Integer.valueOf(properties.getProperty("price.six.month")))) {
                data = properties.getProperty("month.six");
            }
        } else if (update.getCallbackQuery() == null && update.getPreCheckoutQuery() != null) {
            tgId = update.getPreCheckoutQuery().getFrom().getId().toString();
            chatId = update.getPreCheckoutQuery().getFrom().getId().toString();
            data = properties.getProperty("month.one");
            totalAmount = update.getPreCheckoutQuery().getTotalAmount();

            if (totalAmount.equals(Integer.valueOf(properties.getProperty("price.three.month")))) {
                data = properties.getProperty("month.three");
            }
            if (totalAmount.equals(Integer.valueOf(properties.getProperty("price.six.month")))) {
                data = properties.getProperty("month.six");
            }
        } else {
            if (update.getCallbackQuery() == null) {

                return null;
            }
            tgId = update.getCallbackQuery().getFrom().getId().toString();
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            data = update.getCallbackQuery().getData();
        }
        Map<String, OutlineServer> allClients = outlineService.getOutlineServersWithClientsMap(properties);
        Map<String, OutlineClient> serverClient =
                outlineService.getClientByTgId(allClients, tgId);
        if (serverClient == null) {
            log.error("Ошибка получения клиента по tgId '{}'", tgId);

            return null;
        }
        String clientServer = serverClient.keySet().stream().findFirst().isPresent() ?
                serverClient.keySet().stream().findFirst().get() : null;
        OutlineClient outlineClient = serverClient.get(clientServer);
        Instant paidBefore = outlineClient.getPaidBefore();
        Instant now = Instant.now();

        FileEngine.writePaymentToFile(now, totalAmount / 100, tgId, properties);

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

        List<Referral> referrals = FileEngine.getReferrals(properties);
        SendMessage messageToReferrer = null;
        SendMessage messageToReferral = null;
        long referralId = Long.parseLong(outlineClient.getName());
        Referral referral = referrals.stream().filter(r -> r.getReferralId() == referralId).findFirst().orElse(null);
        //если это приглашенный пользователь, добавляем время подписки пригласителю
        if (referral != null && !referral.isActivated()) {
            Map<String, OutlineClient> referrer = outlineService.getClientByTgId(allClients, String.valueOf(referral.getReferrerId()));
            String referrerServer = referrer.keySet().stream().findFirst().orElse(null);
            OutlineClient referrerClient = referrer.get(referrerServer);
            Map<String, OutlineClient> referralClientServerMap = outlineService.getClientByTgId(allClients, String.valueOf(referral.getReferralId()));
            OutlineClient referralClient = referralClientServerMap.get(referralClientServerMap.keySet().stream().findFirst().orElse(null));
            String referralName = getNameForMessage(referralClient);
            Instant referrerPaidBefore = referrerClient.getPaidBefore().isAfter(Instant.now()) ?
                    LocalDateTime
                            .ofInstant(referrerClient.getPaidBefore(), ZoneOffset.UTC)
                            .plusMonths(Integer.parseInt(data)).toInstant(ZoneOffset.UTC) :
                    LocalDateTime
                            .ofInstant(Instant.now(), ZoneOffset.UTC)
                            .plusMonths(Integer.parseInt(data)).toInstant(ZoneOffset.UTC);
            outlineService.updatePaidBefore(
                    referrerServer,
                    referrerPaidBefore
                    ,
                    referrerClient.getId().toString()
            );
            outlineService.enableClient(referrerServer, referrerClient.getId().toString());
            FileEngine.writeReferralToFile(new Referral(referralId, Long.parseLong(referrerClient.getName()), Integer.parseInt(data), true, Instant.now()), properties);
            messageToReferrer = new SendMessage(referrerClient.getName(), """
                    Ура!
                                            
                    Пользователь %s, приглашенный вами, оплатил подписку на %s мес.
                                            
                    Мы подарили вам подписку на %s мес., до %s""".formatted(
                    referralName, data, data, DataTimeUtil.getNovosibirskTimeFromInstant(referrerPaidBefore)
            ));

            String referrerName = getNameForMessage(referrerClient);


            messageToReferral = new SendMessage(referralClient.getName(), """
                    Мы подарили пользователю %s, пригласившему вас, %s мес. подписки.
                                        
                    Чтобы получить бесплатную подписку, нажмите /loyalty""".formatted(
                    referrerName, data
            ));
        }
        var text = "Доступ оплачен до " +
                DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) + "." +
                "\n" +
                "\n" +
                "Если вам необходим чек, обратитесь в нашу дружелюбную поддержку " +
                properties.getProperty("tg.support.user.name");

        List<BotApiMethod<?>> resultMessages = new ArrayList<>(List.of(new SendMessage(chatId, text)));
        if (messageToReferrer != null) {
            resultMessages.add(messageToReferrer);
        }
        if (messageToReferral != null) {
            resultMessages.add(messageToReferral);
        }

        return resultMessages;
    }

    private static String getNameForMessage(OutlineClient referrerClient) {
        String referrerName = referrerClient.getName();
        if (referrerClient.getTgFirst() != null && !referrerClient.getTgFirst().equals("null")
                && referrerClient.getTgLast() != null && !referrerClient.getTgLast().equals("null"))
            referrerName = referrerClient.getTgFirst() + " " + referrerClient.getTgFirst();
        if (referrerClient.getTgLogin() != null && !referrerClient.getTgLogin().equals("null")) {
            referrerName = "@" + referrerClient.getTgLogin();
        }

        return referrerName;
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
                .text(Integer.valueOf(properties.getProperty("month.one")) +
                        " мес.  — " +
                        Integer.parseInt(properties.getProperty("price.one.month")) /
                                100 +
                        "₽")
                .callbackData(properties.getProperty("month.one"))
                .build();

        InlineKeyboardButton three = InlineKeyboardButton.builder()
                .text(properties.getProperty("month.three") +
                        " мес.  — " +
                        Integer.parseInt(properties.getProperty(
                                "price.three.month")) / 100 +
                        "₽")
                .callbackData(properties.getProperty("month.three"))
                .build();

        List<InlineKeyboardButton> keyboardButtonsRowOne = new ArrayList<>();
        keyboardButtonsRowOne.add(one);
        keyboardButtonsRowOne.add(three);

        List<InlineKeyboardButton> keyboardButtonsRowTwo = new ArrayList<>();
        InlineKeyboardButton six = InlineKeyboardButton
                .builder()
                .text(properties.getProperty("month.six") +
                        " мес.  — " +
                        Integer.parseInt(properties.getProperty("price.six.month")) / 100 +
                        "₽")
                .callbackData(properties.getProperty("month.six"))
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

    @Override
    public Map<Long, List<PartialBotApiMethod<Message>>> sendMassMessages(Update update) {

        return null;
    }
}
