package bennyhils.inc.tgbot;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.action.Instruction;
import bennyhils.inc.tgbot.model.MassMessage;
import bennyhils.inc.tgbot.model.receipt.Amount;
import bennyhils.inc.tgbot.model.receipt.Item;
import bennyhils.inc.tgbot.model.receipt.Receipt;
import bennyhils.inc.tgbot.util.FileEngine;
import bennyhils.inc.tgbot.util.JsonParserUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BotMenu extends TelegramLongPollingBot {
    private final Map<String, String> bindingUsersActionsBy = new ConcurrentHashMap<>();
    private final Map<String, String> bindingAdminsActionsBy = new ConcurrentHashMap<>();
    private final Map<String, Action> usersActions;
    private final Map<String, Action> adminsActions;
    private final Properties properties;

    public BotMenu(
            Map<String, Action> usersActions,
            Map<String, Action> adminsActions,
            Properties properties
    ) {
        this.usersActions = usersActions;
        this.adminsActions = adminsActions;
        this.properties = properties;
    }

    public String getBotUsername() {
        return properties.getProperty("tg.username");
    }

    public String getBotToken() {
        return properties.getProperty("tg.token");
    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage() != null && update.getMessage().hasSuccessfulPayment()) {
            BotApiMethod<?> callback = usersActions.get("/buy").callback(update);
            sendMsg(callback);
            bindingUsersActionsBy.remove(update.getMessage().getChatId().toString());
        }

        // User section
        if (update.hasMessage()) {
            var key = update.getMessage().getText();
            var chatId = update.getMessage().getChatId().toString();

            if (key != null && usersActions.containsKey(key)) {
                var msg = usersActions.get(key).handle(update);
                bindingUsersActionsBy.put(chatId, key);
                sendMsg(msg);
            } else if (bindingUsersActionsBy.containsKey(chatId) && !bindingUsersActionsBy.containsValue("/buy")) {
                var msg = usersActions.get(bindingUsersActionsBy.get(chatId)).callback(update);
                if (msg != null) {
                    sendMsg(msg);
                }
                var doc = usersActions.get(bindingUsersActionsBy.get(chatId)).sendDocument(update);
                bindingUsersActionsBy.remove(chatId);
                if (doc != null) {
                    sendDocument(doc);
                }
            }
        } else if (update.hasCallbackQuery()) {
            var chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();

            if (data.equals(properties.getProperty("month.three"))) {
                sendInvoice(
                        chatId.toString(),
                        "На " + Integer.valueOf(properties.getProperty("month.three")) + " месяца",
                        Integer.valueOf(properties.getProperty("price.three.month"))
                );
            }
            if (data.equals(properties.getProperty("month.six"))) {

                sendInvoice(
                        chatId.toString(),
                        "На " + Integer.valueOf(properties.getProperty("month.six")) + " месяцев",
                        Integer.valueOf(properties.getProperty("price.six.month"))
                );
            }
            if (data.equals(properties.getProperty("month.one"))) {
                sendInvoice(
                        chatId.toString(),
                        "На " + Integer.valueOf(properties.getProperty("month.one")) + " месяц",
                        Integer.valueOf(properties.getProperty("price.one.month"))
                );
            }
            switch (data) {
                case (Instruction.IOS), (Instruction.ANDROID) -> {
                    bindingUsersActionsBy.remove(chatId.toString());
                    sendMsg(usersActions.get("/instruction").callback(update));
                    sendVideo(usersActions.get("/instruction").sendVideo(update));
                }
            }

        } else if (update.hasPreCheckoutQuery()) {
            PreCheckoutQuery preCheckoutQuery = update.getPreCheckoutQuery();
            AnswerPreCheckoutQuery answerPreCheckoutQuery =
                    new AnswerPreCheckoutQuery(preCheckoutQuery.getId(), true);
            sendMsg(answerPreCheckoutQuery);
        }

        // Admin section
        if (update.hasMessage() &&
                update.getMessage().hasPhoto() ||
                (update.hasMessage() && update.getMessage().hasText() &&
                        update.getMessage().getText() != null) &&
                        JsonParserUtil.getStringArray("tg.admin.ids", properties) != null &&
                        JsonParserUtil
                                .getStringArray("tg.admin.ids", properties)
                                .contains(update.getMessage().getFrom().getId().toString())) {

            var key = update.getMessage().getText();
            var chatId = update.getMessage().getChatId().toString();

            if (key != null && adminsActions.containsKey(key)) {
                var msg = adminsActions.get(key).handle(update);
                bindingAdminsActionsBy.put(chatId, key);
                sendMsg(msg);
            } else if (bindingAdminsActionsBy.containsKey(chatId) &&
                    update.getMessage() != null) {
                var msg = adminsActions.get(bindingAdminsActionsBy.get(chatId)).callback(update);
                if (update.getMessage().getPhoto() != null && bindingAdminsActionsBy.containsValue("/m")) {
                    GetFile getFile = new GetFile(update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId());
                    File execute = null;
                    try {
                        execute = execute(getFile);
                    } catch (TelegramApiException e) {
                        log.error("Не удалось получить файл: '{}'", e.getMessage());
                    }
                    try {
                        long id = getNextMMid(properties);
                        int picId = (int) id;

                        downloadFile(
                                execute,
                                new java.io.File(properties.getProperty("mass.messages.folder") +
                                        java.io.File.separator +
                                        properties.getProperty("mass.messages.photos.folder") +
                                        java.io.File.separator +
                                        picId +
                                        properties.getProperty("mass.messages.photo.png"))
                        );
                        FileEngine.writeMassMessageToFile(
                                id,
                                null,
                                update.getMessage().getCaption(),
                                picId,
                                0L,
                                0L,
                                properties
                        );
                        sendMsg(new SendMessage(update.getMessage().getChatId().toString(),
                                "Сохранено массовое сообщение с картинкой"));
                    } catch (TelegramApiException e) {
                        log.error("Не удалось получить файл: '{}'", e.getMessage());
                    }
                }
                if (msg != null) {
                    sendMsg(msg);
                }
                var doc = adminsActions.get(bindingAdminsActionsBy.get(chatId)).sendDocument(update);
                if (doc != null) {
                    sendDocument(doc);
                }
                var mm = adminsActions.get(bindingAdminsActionsBy.get(chatId)).sendMassMessages(update);
                if (mm != null && !mm.isEmpty()) {
                    Long mId = mm.keySet().stream().findFirst().orElse(null);
                    int sentTo = mm.get(mId).size();
                    int deliveredTo = 0;
                    for (PartialBotApiMethod<Message> p : mm.get(mId)) {
                        if (p != null) {
                            SendPhoto sendPhoto = (SendPhoto) p;
                            if (sendPhoto.getPhoto().getNewMediaFile() !=
                                    null && !sendPhoto.getPhoto().getMediaName().equals(properties.getProperty("mass.messages.photo.no"))) {
                                if (sendPhoto(p)) {
                                    deliveredTo++;
                                }
                            } else {
                                if (sendMsg(new SendMessage(((SendPhoto) p).getChatId(), ((SendPhoto) p).getCaption()))) {
                                    deliveredTo++;

                                }
                            }
                        }
                    }
                    List<MassMessage> allMassMessages = FileEngine.getAllMassMessages(properties);
                    if (allMassMessages != null) {
                        MassMessage massMessage = allMassMessages.stream().filter(massM -> massM.getId() == mId).findFirst().orElse(null);
                        if (massMessage != null) {
                            FileEngine.writeMassMessageToFile(mId, Instant.now(), massMessage.getMessage(), massMessage.getPicId(), sentTo, deliveredTo, properties);
                        }
                    } else {
                        log.error("Ошибка поиска массовых сообщений");
                    }
                }

                bindingAdminsActionsBy.remove(chatId);
            }
        } else if (update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText() != null &&
                adminsActions.containsKey(update.getMessage().getText()) &&
                JsonParserUtil.getStringArray("tg.admin.ids", properties) != null &&
                !JsonParserUtil
                        .getStringArray("tg.admin.ids", properties)
                        .contains(update.getMessage().getFrom().getId().toString())) {
            sendMsg(new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Вы не администратор, не жмите эти кнопки!"
            ));
        }
    }

    public static long getNextMMid(Properties properties) {
        List<MassMessage> allMassMessages = FileEngine.getAllMassMessages(properties);
        long id;
        if (allMassMessages == null || allMassMessages.isEmpty()) {
            id = 1L;
        } else {
            id = Collections.max(allMassMessages, Comparator.comparing(MassMessage::getId)).getId() + 1;
        }

        return id;
    }

    public boolean sendMsg(BotApiMethod<?> msg) {
        try {
            execute(msg);

            return true;
        } catch (TelegramApiException e) {
            log.error(
                    "Не удалось отправить сообщение клиенту с tgId '{}' с ошибкой: '{}'",
                    ((SendMessage) msg).getChatId(),
                    e.getMessage()
            );

            return false;
        }
    }

    private void sendDocument(PartialBotApiMethod<Message> msg) {
        try {
            execute((SendDocument) msg);
        } catch (TelegramApiException e) {
            log.error(
                    "Не удалось отправить документ клиенту с tgId '{}' с ошибкой: '{}'",
                    ((SendDocument) msg).getChatId(),
                    e.getMessage()
            );
        }
    }

    private void sendVideo(PartialBotApiMethod<Message> msg) {
        try {
            execute((SendVideo) msg);
        } catch (TelegramApiException e) {
            log.error(
                    "Не удалось отправить видео клиенту с tgId '{}' с ошибкой: '{}'",
                    ((SendVideo) msg).getChatId(),
                    e.getMessage()
            );
        }
    }

    private boolean sendPhoto(PartialBotApiMethod<Message> msg) {
        try {
            execute((SendPhoto) msg);

            return true;
        } catch (TelegramApiException e) {
            log.error(
                    "Не удалось отправить фото клиенту с tgId '{}' с ошибкой: '{}'",
                    ((SendPhoto) msg).getChatId(),
                    e.getMessage()
            );

            return false;
        }
    }

    private void sendInvoice(String chatId, String description, Integer price) {
        String rub = "RUB";
        String descriptionForProvData = "Доступ к VPN. " + description;
        SendInvoice sendInvoice = new SendInvoice(
                chatId,
                "Доступ к VPN",
                description,
                UUID.randomUUID().toString(),
                properties.getProperty("provider.token"),
                "1",
                "RUB",
                Collections.singletonList(new LabeledPrice("Цена", price))
        );

        Amount amount = new Amount(price / 100 + ".00", rub);
        Item item = new Item(descriptionForProvData, "1.00", amount, 1);
        Receipt receipt = new Receipt(properties.getProperty("mail"), Collections.singletonList(item));

        ObjectWriter ow = new ObjectMapper().writer().withRootName("receipt").withDefaultPrettyPrinter();
        String json = null;
        try {
            json = ow.writeValueAsString(receipt);
        } catch (JsonProcessingException e) {
            log.error("Не удалось преобразовать объект receipt из JSON в String с ошибкой: '{}'", e.getMessage());
        }
        sendInvoice.setProviderData(json);
        sendMsg(sendInvoice);
    }
}
