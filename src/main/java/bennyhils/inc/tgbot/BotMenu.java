package bennyhils.inc.tgbot;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.action.Buy;
import bennyhils.inc.tgbot.action.Instruction;
import bennyhils.inc.tgbot.model.receipt.Amount;
import bennyhils.inc.tgbot.model.receipt.Item;
import bennyhils.inc.tgbot.model.receipt.Receipt;
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
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
    public class BotMenu extends TelegramLongPollingBot {
    private final Map<String, String> bindingBy = new ConcurrentHashMap<>();
    private final Map<String, Action> actions;
    private final String username;
    private final String token;
    private final String providerToken;
    private final String mail;

    public BotMenu(Map<String, Action> actions, String username, String token, String providerToken, String mail) {
        this.actions = actions;
        this.username = username;
        this.token = token;
        this.providerToken = providerToken;
        this.mail = mail;
    }

    public String getBotUsername() {
        return username;
    }

    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            var key = update.getMessage().getText();
            var chatId = update.getMessage().getChatId().toString();
            if (key != null && actions.containsKey(key)) {
                var msg = actions.get(key).handle(update);
                bindingBy.put(chatId, key);
                sendMsg(msg);
                bindingBy.remove(chatId);
            } else if (bindingBy.containsKey(chatId)) {
                var msg = actions.get(bindingBy.get(chatId)).callback(update);
                if (msg != null) {
                    sendMsg(msg);
                }
                var doc = actions.get(bindingBy.get(chatId)).sendDocument(update);
                bindingBy.remove(chatId);
                sendDocument(doc);
            }
        } else if (update.hasCallbackQuery()) {
            var chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();

            if (data.equals(Buy.ONE) ||
                data.equals(Buy.THREE) ||
                data.equals(Buy.SIX) ||
                data.equals(Buy.TWELVE)) {
                SendInvoice sendInvoice = switch (data) {
                    case (Buy.THREE) -> createSendInvoice(
                            chatId.toString(),
                            "На " + Buy.THREE + " месяца",
                            providerToken,
                            Buy.THREE_PRICE
                    );
                    case (Buy.SIX) -> createSendInvoice(
                            chatId.toString(),
                            "На " + Buy.SIX + " месяцев",
                            providerToken,
                            Buy.SIX_PRICE
                    );
                    case (Buy.TWELVE) -> createSendInvoice(
                            chatId.toString(),
                            "На " + Buy.TWELVE + " месяцев",
                            providerToken,
                            Buy.TWELVE_PRICE
                    );
                    default -> createSendInvoice(
                            chatId.toString(),
                            "На " + Buy.ONE + " месяц",
                            providerToken,
                            Buy.ONE_PRICE
                    );
                };


                sendMsg(sendInvoice);

            } else if (data.equals(
                    Instruction.ANDROID) || data.equals(Instruction.IOS)
            ) {
                sendMsg(actions.get("/instruction").callback(update));
                sendDocument(actions.get("/instruction").sendDocument(update));
                sendVideo(actions.get("/instruction").sendVideo(update));
            }
        }

        if (update.hasPreCheckoutQuery()) {
            PreCheckoutQuery preCheckoutQuery = update.getPreCheckoutQuery();
            AnswerPreCheckoutQuery answerPreCheckoutQuery =
                    new AnswerPreCheckoutQuery(preCheckoutQuery.getId(), true);
            sendMsg(answerPreCheckoutQuery);
        }
        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            BotApiMethod<?> callback = actions.get("/buy").callback(update);
            sendMsg(callback);
        }
    }

    public void sendMsg(BotApiMethod<?> msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение: {}", e.getMessage());
        }
    }

    private void sendDocument(PartialBotApiMethod<Message> msg) {
        try {
            execute((SendDocument) msg);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить документ: {}", e.getMessage());
        }
    }

    private void sendVideo(PartialBotApiMethod<Message> msg) {
        try {
            execute((SendVideo) msg);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить видео: {}", e.getMessage());
        }
    }

    private SendInvoice createSendInvoice(String chatId, String description, String providerToken, Integer price) {
        String rub = "RUB";
        String descriptionForProvData = "Доступ к VPN. " + description;
        SendInvoice sendInvoice = new SendInvoice(
                chatId,
                "Доступ к VPN",
                description,
                UUID.randomUUID().toString(),
                providerToken,
                "1",
                "RUB",
                Collections.singletonList(new LabeledPrice("Цена", price))
        );

        Amount amount = new Amount(price / 100 + ".00", rub);
        Item item = new Item(descriptionForProvData, "1.00", amount, 1);
        Receipt receipt = new Receipt(mail, Collections.singletonList(item));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = null;
        try {
            json = ow.writeValueAsString(receipt);
        } catch (JsonProcessingException e) {
            log.error("Не удалось преобразовать объект в JSON: {}", e.getMessage());
        }

        sendInvoice.setProviderData(json);

        return sendInvoice;
    }
}
