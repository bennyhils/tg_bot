package bennyhils.inc.tgbot;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.action.Buy;
import bennyhils.inc.tgbot.action.Instruction;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.receipt.Amount;
import bennyhils.inc.tgbot.model.receipt.Item;
import bennyhils.inc.tgbot.model.receipt.Receipt;
import bennyhils.inc.tgbot.util.JsonParserUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    OutlineService outlineService = new OutlineService();

    public String getBotUsername() {
        return properties.getProperty("tg.username");
    }

    public String getBotToken() {
        return properties.getProperty("tg.token");
    }


    @Override
    public void onUpdateReceived(Update update) {

        // User section
        if (update.hasMessage()) {
            var key = update.getMessage().getText();
            var chatId = update.getMessage().getChatId().toString();

            if (key != null && usersActions.containsKey(key)) {
                var msg = usersActions.get(key).handle(update);
                bindingUsersActionsBy.put(chatId, key);
                sendMsg(msg);
            } else if (bindingUsersActionsBy.containsKey(chatId)) {
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

            switch (data) {
                case (Buy.THREE_MONTHS) -> sendInvoice(
                        chatId.toString(),
                        "На " + Buy.THREE_MONTHS + " месяца",
                        Buy.THREE_MONTHS_PRICE
                );

                case (Buy.SIX_MONTHS) -> sendInvoice(
                        chatId.toString(),
                        "На " + Buy.SIX_MONTHS + " месяцев",
                        Buy.SIX_MONTHS_PRICE
                );

                case (Instruction.IOS), (Instruction.ANDROID) -> {
                    bindingUsersActionsBy.remove(chatId.toString());
                    sendMsg(usersActions.get("/instruction").callback(update));
                    sendVideo(usersActions.get("/instruction").sendVideo(update));
                }

                default -> sendInvoice(
                        chatId.toString(),
                        "На " + Buy.ONE_MONTH + " месяц",
                        Buy.ONE_MONTH_PRICE
                );
            }

        } else if (update.hasPreCheckoutQuery()) {
            PreCheckoutQuery preCheckoutQuery = update.getPreCheckoutQuery();
            AnswerPreCheckoutQuery answerPreCheckoutQuery =
                    new AnswerPreCheckoutQuery(preCheckoutQuery.getId(), true);
            sendMsg(answerPreCheckoutQuery);
        }

        // Admin section
        if (update.hasMessage() &&
            update.getMessage().hasText() &&
            update.getMessage().getText() != null &&
            JsonParserUtil.getStringArray("tg.admin.ids", properties) != null &&
            JsonParserUtil
                    .getStringArray("tg.admin.ids", properties)
                    .contains(update.getMessage().getFrom().getId().toString())) {

            var key = update.getMessage().getText();
            var chatId = update.getMessage().getChatId().toString();

            if (key != null && adminsActions.containsKey(key) && !key.equals("/sendMigrationMessage")) {
                var msg = adminsActions.get(key).handle(update);
                bindingAdminsActionsBy.put(chatId, key);
                sendMsg(msg);
            } else if (bindingAdminsActionsBy.containsKey(chatId) &&
                       update.getMessage() != null) {
                var msg = adminsActions.get(bindingAdminsActionsBy.get(chatId)).callback(update);
                if (msg != null) {
                    sendMsg(msg);
                }
                var doc = adminsActions.get(bindingAdminsActionsBy.get(chatId)).sendDocument(update);
                if (doc != null) {
                    sendDocument(doc);
                }
                bindingAdminsActionsBy.remove(chatId);

            }

            if (key != null && adminsActions.containsKey(key) && key.equals("/sendMigrationMessage")) {
                List<OutlineClient> outlineClients = outlineService.getAllServersClients(properties);

                for (String c : outlineClients.stream().map(OutlineClient::getName).collect(Collectors.toSet())
                ) {

                    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                    InputStream is;
                    is = classloader.getResourceAsStream("Migration.png");

                    String name = "друг";

                    OutlineClient outlineClient = outlineClients
                            .stream()
                            .filter(oc -> oc.getName().equals(c))
                            .findFirst()
                            .orElse(null);

                    if (outlineClient != null) {
                        name = outlineClient.getTgLogin().equals("null") ? "друг" : "@" + outlineClient.getTgLogin();
                    }

                    String idSendTo = "96902655";
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        log.error("Не удалось вставить паузу между отправками сообщений клиенту с tgId: '{}'!", idSendTo);
                    }
                    SendPhoto photo = new SendPhoto(
                            idSendTo,
                            new InputFile(is, "Мигрируем с WireGuard на Outline")
                    );
                    photo.setCaption("""
                            Привет, %s :)
                                                    
                            Последнее время приложение WireGuard работает плохо. Скорее всего вы это заметили и даже обратились в поддержку. Спасибо вам.
                                                    
                            Мы решили проблему на корню — сменили приложение на более быстрое, безопасное и стабильное, а главное, которое не заблокируешь!
                                                    
                            Отключаем, закрываем и удаляем <strike>WireGuard</strike> -> скачиваем, открываем и подключаем <b>Outline</b>!
                                                    
                            Мы дарим вам 1 неделю бесплатного доступа за то, что вам придется сменить приложение. Всем! Даже тем, кто ни разу не пользовался. Стоит попробовать.
                             
                            Тем, кто уже получил ключ через поддержку — ничего делать не надо, для вас миграция бесшовна.
                                                        
                            Чтобы подключиться, нажмите /instruction
                            """
                            .formatted(name));


                    photo.setParseMode("HTML");
                    log.info("Отправляем сообщение клиенту с tgId: '{}'", idSendTo);
                    sendPhoto(photo);
                }
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

    public void sendMsg(BotApiMethod<?> msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error(
                    "Не удалось отправить сообщение клиенту с tgId '{}' с ошибкой: '{}'",
                    ((SendMessage) msg).getChatId(),
                    e.getMessage()
            );
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

    private void sendPhoto(PartialBotApiMethod<Message> msg) {
        try {
            execute((SendPhoto) msg);
        } catch (TelegramApiException e) {
            log.error(
                    "Не удалось отправить фото клиенту с tgId '{}' с ошибкой: '{}'",
                    ((SendPhoto) msg).getChatId(),
                    e.getMessage()
            );
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
