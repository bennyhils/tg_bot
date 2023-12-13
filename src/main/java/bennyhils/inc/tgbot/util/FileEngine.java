package bennyhils.inc.tgbot.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bennyhils.inc.tgbot.model.MassMessage;
import bennyhils.inc.tgbot.model.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

@Slf4j
public class FileEngine {
    final static String PAYMENTS_FOLDER = "payments";
    final static String MASS_MESSAGES_FOLDER = "mass_messages";
    static ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static void writePaymentToFile(Instant now, Integer amount, String tgId) {
        Payment paymentObj = new Payment(tgId, amount, now.toEpochMilli());

        try {
            File file = new File(PAYMENTS_FOLDER +
                                 File.separator +
                                 DataTimeUtil.getFileNameForCheck(now) +
                                 "_" +
                                 tgId +
                                 ".json");
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));
            printWriter.println(objectMapper.writeValueAsString(paymentObj));
            printWriter.close();
        } catch (IOException e) {
            log.error("Ошибка при работе с файлом: {}", e.getMessage());
        }

    }

    public static void writeMassMessageToFile(long id, String message, long picId, long sentTo, long deliveredTo) {
        MassMessage massMessage = new MassMessage(id, message, picId, sentTo, deliveredTo);

        try {
            File file = new File(MASS_MESSAGES_FOLDER +
                                 File.separator +
                                 id +
                                 ".json");
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));
            printWriter.println(objectMapper.writeValueAsString(massMessage));
            printWriter.close();
        } catch (IOException e) {
            log.error("Ошибка при работе с файлом: {}", e.getMessage());
        }

    }

    public static void writeMassMessagePhotoToFile(PhotoSize photoSize) {

        try {
            File file = new File(MASS_MESSAGES_FOLDER +
                                 File.separator +
                                 photoSize.getFileId() +
                                 ".jpeg");
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));
            printWriter.println(photoSize);
            printWriter.close();
        } catch (IOException e) {
            log.error("Ошибка при работе с файлом: {}", e.getMessage());
        }

    }

    public static Map<Long, Long> getTotalAndLastThreeMPayments() {
        List<Payment> allPayments = getAllPayments();
        Map<Long, Long> result = new HashMap<>();
        Instant now = Instant.now();
        ZonedDateTime zonedUTC = now.atZone(ZoneId.of("UTC"));
        ZonedDateTime nsk = zonedUTC.withZoneSameInstant(ZoneId.of("Asia/Novosibirsk"));
        Month currentMonth = nsk.getMonth();
        int currentMTotalAmount = 0;
        int currentMminus1TotalAmount = 0;
        int currentMminus2TotalAmount = 0;
        int total = 0;

        for (Payment p : allPayments) {
            Instant paymentTime = Instant.ofEpochMilli(p.getPaymentEpochMilli());
            ZonedDateTime paymentZonedIST = paymentTime.atZone(ZoneId.of("UTC"));
            Month paymentMonth = paymentZonedIST.getMonth();
            if (paymentMonth.getValue() == currentMonth.getValue()) {
                currentMTotalAmount = currentMTotalAmount + p.getAmount();
            } else if (paymentMonth.getValue() == currentMonth.getValue() - 1) {
                currentMminus1TotalAmount = currentMminus1TotalAmount + p.getAmount();
            } else if (paymentMonth.getValue() == currentMonth.getValue() - 2) {
                currentMminus2TotalAmount = currentMminus2TotalAmount + p.getAmount();
            }
            total = total + p.getAmount();
        }
        result.put((long) currentMonth.getValue(), (long) currentMTotalAmount);
        result.put((long) currentMonth.minus(1L).getValue(), (long) currentMminus1TotalAmount);
        result.put((long) currentMonth.minus(2L).getValue(), (long) currentMminus2TotalAmount);
        result.put(0L, (long) total);
        return result;
    }

    public static List<Payment> getAllPayments() {
        final File folder = new File(PAYMENTS_FOLDER);
        List<Payment> payments = new ArrayList<>();
        if (folder.exists() && folder.listFiles() != null && folder.listFiles().length != 0) {
            for (final File fileEntry : folder.listFiles()) {
                try {
                    Payment payment = objectMapper.readValue(new File(PAYMENTS_FOLDER +
                                                                      File.separator +
                                                                      fileEntry.getName()), Payment.class);
                    payments.add(payment);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            log.info("Не было ни одной оплаты");
        }
        return payments;
    }

    public static List<MassMessage> getAllMassMessages() {
        final File folder = new File(MASS_MESSAGES_FOLDER);
        List<MassMessage> massMessages = new ArrayList<>();
        if (folder.exists() && folder.listFiles() != null && folder.listFiles().length != 0) {
            for (final File fileEntry : folder.listFiles()) {
                if (!fileEntry.isDirectory()) {
                    try {
                        MassMessage massMessage = objectMapper.readValue(
                                new File(MASS_MESSAGES_FOLDER +
                                         File.separator +
                                         fileEntry.getName()),
                                MassMessage.class
                        );
                        massMessages.add(massMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            log.info("Не было ни одного массового сообщения");
        }
        return massMessages;
    }
}