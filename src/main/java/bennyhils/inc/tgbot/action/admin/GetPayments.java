package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.Payment;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.util.FileEngine;
import bennyhils.inc.tgbot.vpn.OutlineService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
public class GetPayments implements Action {

    public final static String TOTAL_PAYMENTS_KEY = "totalPaymentsKey";

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public GetPayments(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {

        Map<String, Long> payments = FileEngine.getTotalAndLastThreeMPayments(properties);
        if (payments == null || payments.isEmpty()) {

            return new SendMessage(update.getMessage().getChatId().toString(), "Не было ни одной оплаты");
        }
        SortedSet<String> keys = new TreeSet<>(payments.keySet());
        keys.remove(TOTAL_PAYMENTS_KEY);
        StringBuilder msg = new StringBuilder();
        msg.append("Оплаты за последние 3 месяца: \n\n");
        for (String key : Lists.reverse(keys.stream().toList())) {
            Long value = payments.get(key);
            Month month = Month.of(Math.toIntExact(Long.parseLong(key.split("\\.")[1])));
            msg
                    .append(month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru-RU")))
                    .append(": ")
                    .append(value)
                    .append("₽")
                    .append("\n");
        }

        msg.append("\nОплат за все время: ").append(payments.get(TOTAL_PAYMENTS_KEY)).append("₽\n");

        msg.append("\nЧтобы получить данные за конкретный месяц, введите его в формате: <code>2023 11</code>.")
                .append("\n\nИли выслать файл со всеми оплатами?");


        SendMessage sendMessage = new SendMessage(update.getMessage().getChatId().toString(), msg.toString());
        sendMessage.enableHtml(true);
        return sendMessage;
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        if (!update.hasMessage()) {

            return null;
        }
        if (!update.getMessage().hasText()) {

            return null;
        }
        String message = update.getMessage().getText();
        String[] parts = message.split("[,\\s]+");
        if (parts.length == 1) {

            return null;
        }
        List<Payment> payments = FileEngine.getAllPayments(properties);
        int year;
        int month;
        try {
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            log.warn("Введена неправильная команда поиска оплат!");

            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Введена неправильная команда для поиска оплат!"
            );
        }
        long total = 0;
        if (payments == null || payments.isEmpty()) {

            return new SendMessage(update.getMessage().getChatId().toString(), "Не было ни одной оплаты");
        }
        for (Payment p : payments) {
            Instant paymentTime = Instant.ofEpochMilli(p.getPaymentEpochMilli());
            ZonedDateTime paymentZonedIST = paymentTime.atZone(ZoneId.of("UTC"));
            if (paymentZonedIST.getYear() == year && paymentZonedIST.getMonth().getValue() == month) {
                total = total + p.getAmount();
            }

        }

        return new SendMessage(
                update.getMessage().getChatId().toString(),
                year + "." + month + " оплат на " + total + "₽"
        );
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {
        if (!update.hasMessage()) {

            return null;
        }
        if (!update.getMessage().hasText()) {

            return null;
        }
        if (update.getMessage().getText().equals(properties.getProperty("tg.admin.yes.word"))) {
            InputStream targetStream;
            List<Payment> payments = FileEngine.getAllPayments(properties);
            if (payments == null || payments.isEmpty()) {

                return null;
            }
            payments.sort(Comparator.comparing(Payment::getPaymentEpochMilli));
            payments = Lists.reverse(payments);
            StringBuilder result = new StringBuilder();
            result.append("Оплаты: ").append(" \n");
            result.append(
                    "    #, TgId, TgLogin, Имя, Фамилия, Оплатил ₽, Дата оплаты \n");
            List<OutlineClient> allServersClients = outlineService.getAllServersClients(properties);
            int i = 1;
            for (Payment p : payments) {
                OutlineClient client = allServersClients
                        .stream()
                        .filter(c -> c.getName().equals(p.getTgId()))
                        .findFirst()
                        .orElse(new OutlineClient());
                result
                        .append("\n    ")
                        .append(i)
                        .append(", ")
                        .append(p.getTgId())
                        .append(", ")
                        .append(client.getTgLogin() != null ? client.getTgLogin() : "NOT FOUND")
                        .append(", ")
                        .append(client.getTgFirst() != null ? client.getTgFirst() : "NOT FOUND")
                        .append(", ")
                        .append(client.getTgLast() != null ? client.getTgLast() : "NOT FOUND")
                        .append(", ")
                        .append(p.getAmount())
                        .append(", ")
                        .append(DataTimeUtil
                                .getFileNameForCheck(Instant.ofEpochMilli(p.getPaymentEpochMilli())))
                        .append("\n");
                i++;
            }
            targetStream = new ByteArrayInputStream(result.toString().getBytes());
            SendDocument sendDocument = new SendDocument(
                    update.getMessage().getChatId().toString(),
                    new InputFile(targetStream, "Все оплаты.txt")
            );
            sendDocument.setCaption("Оплаты");

            return sendDocument;
        } else {

            return null;
        }
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
