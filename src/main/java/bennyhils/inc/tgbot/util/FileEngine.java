package bennyhils.inc.tgbot.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;

import bennyhils.inc.tgbot.model.MassMessage;
import bennyhils.inc.tgbot.model.Payment;
import bennyhils.inc.tgbot.model.Referral;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileEngine {
    public final static String TOTAL_PAYMENTS_KEY = "totalPaymentsKey";
    public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public static void writePaymentToFile(Instant now, Integer amount, String tgId, Properties properties) {
        Payment paymentObj = new Payment(tgId, amount, now.toEpochMilli());

        try {
            File file = new File(properties.getProperty("payments.folder") +
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
            printWriter.println(OBJECT_MAPPER.writeValueAsString(paymentObj));
            printWriter.close();
        } catch (IOException e) {
            log.error("Ошибка при работе с файлом: {}", e.getMessage());
        }
    }

    public static void writeReferralToFile(Referral referral, Properties properties) {

        try {
            File file = new File(properties.getProperty("referrals.folder") +
                    File.separator +
                    referral.getReferralId() +
                    ".json");
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                file.createNewFile();
                PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));
                printWriter.println(OBJECT_MAPPER.writeValueAsString(referral));
                printWriter.close();
            } else {
                file.delete();
                file.createNewFile();
                PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));
                printWriter.println(OBJECT_MAPPER.writeValueAsString(referral));
                printWriter.close();
            }
        } catch (IOException e) {
            log.error("Ошибка при работе с файлом: {}", e.getMessage());
        }
    }

    public static List<Referral> getReferrals(Properties properties) {
        final File folder = new File(properties.getProperty("referrals.folder"));
        List<Referral> referrals = new ArrayList<>();
        if (folder.exists() && folder.listFiles() != null && folder.listFiles().length != 0) {
            for (final File fileEntry : folder.listFiles()) {
                try {
                    Referral referral = OBJECT_MAPPER.readValue(new File(properties.getProperty("referrals.folder") +
                            File.separator +
                            fileEntry.getName()), Referral.class);
                    referrals.add(referral);
                } catch (IOException e) {
                    log.error("Ошибка при работе с файлом: {}", e.getMessage());

                    return Collections.emptyList();
                }
            }
        } else {
            log.info("Никто не зарегистрировался в программе лояльности");

            return Collections.emptyList();
        }

        return referrals;
    }

    public static List<Referral> getClientReferrals(Properties properties, long tgId) {
        List<Referral> referrals = getReferrals(properties);
        List<Referral> clientReferrals = new ArrayList<>();
        for (Referral r : referrals) {
            if (r.getReferrerId() == tgId) {
                clientReferrals.add(r);
            }
        }

        return clientReferrals;
    }

    public static void writeMassMessageToFile(long id, Instant sendingTime, String message, long picId, long sentTo, long deliveredTo, Properties properties) {
        MassMessage massMessage = new MassMessage(id, message, picId, sendingTime, sentTo, deliveredTo);

        try {
            File file = new File(properties.getProperty("mass.messages.folder") +
                    File.separator +
                    id +
                    ".json");
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));
            printWriter.println(OBJECT_MAPPER.writeValueAsString(massMessage));
            printWriter.close();
        } catch (IOException e) {
            log.error("Ошибка при работе с файлом: {}", e.getMessage());
        }
    }

    public static Map<String, Long> getTotalAndLastThreeMPayments(Properties properties, List<Payment> payments) {
        List<Payment> allPayments = properties != null ? getAllPayments(properties) : payments;
        Map<String, Long> result = new HashMap<>();
        final Instant now = Instant.now();
        ZonedDateTime zonedUTC = now.atZone(ZoneId.of("UTC"));
        ZonedDateTime nsk = zonedUTC.withZoneSameInstant(ZoneId.of("Asia/Novosibirsk"));
        ZonedDateTime nskOneMAgo = nsk.minusMonths(1);
        ZonedDateTime nskTwoMAgo = nsk.minusMonths(2);
        Month currentMonth = nsk.getMonth();
        Month oneMAgoMonth = nskOneMAgo.getMonth();
        Month twoMAgoMonth = nskTwoMAgo.getMonth();
        int currentMTotalAmount = 0;
        int currentMminus1TotalAmount = 0;
        int currentMminus2TotalAmount = 0;
        int total = 0;

        if (allPayments == null) {

            return null;
        }
        for (Payment p : allPayments) {
            Instant paymentTime = Instant.ofEpochMilli(p.getPaymentEpochMilli());
            ZonedDateTime paymentZonedIST = paymentTime.atZone(ZoneId.of("UTC"));
            Month paymentMonth = paymentZonedIST.getMonth();
            if (paymentMonth.getValue() == currentMonth.getValue() && paymentZonedIST.getYear() == nsk.getYear()) {
                currentMTotalAmount = currentMTotalAmount + p.getAmount();
            } else if (paymentMonth.getValue() == oneMAgoMonth.getValue() && paymentZonedIST.getYear() == nskOneMAgo.getYear()) {

                currentMminus1TotalAmount = currentMminus1TotalAmount + p.getAmount();
            } else if (paymentMonth.getValue() == twoMAgoMonth.getValue() && paymentZonedIST.getYear() == nskTwoMAgo.getYear()) {
                currentMminus2TotalAmount = currentMminus2TotalAmount + p.getAmount();
            }
            total = total + p.getAmount();
        }
        result.put(nsk.getYear() + "." + currentMonth.getValue(), (long) currentMTotalAmount);
        result.put(nskOneMAgo.getYear() + "." + oneMAgoMonth.getValue(), (long) currentMminus1TotalAmount);
        result.put(nskTwoMAgo.getYear() + "." + twoMAgoMonth.getValue(), (long) currentMminus2TotalAmount);
        result.put(TOTAL_PAYMENTS_KEY, (long) total);
        return result;
    }

    public static List<Payment> getAllPayments(Properties properties) {
        final File folder = new File(properties.getProperty("payments.folder"));
        List<Payment> payments = new ArrayList<>();
        if (folder.exists() && folder.listFiles() != null && folder.listFiles().length != 0) {
            for (final File fileEntry : folder.listFiles()) {
                try {
                    Payment payment = OBJECT_MAPPER.readValue(new File(properties.getProperty("payments.folder") +
                            File.separator +
                            fileEntry.getName()), Payment.class);
                    payments.add(payment);
                } catch (IOException e) {
                    log.error("Ошибка при работе с файлом: {}", e.getMessage());

                    return null;
                }
            }
        } else {
            log.info("Не было ни одной оплаты");

            return null;
        }

        return payments;
    }

    public static List<MassMessage> getAllMassMessages(Properties properties) {
        final File folder = new File(properties.getProperty("mass.messages.folder"));
        List<MassMessage> massMessages = new ArrayList<>();
        if (folder.exists() && folder.listFiles() != null && folder.listFiles().length != 0) {
            for (final File fileEntry : folder.listFiles()) {
                if (!fileEntry.isDirectory()) {
                    try {
                        MassMessage massMessage = OBJECT_MAPPER.readValue(
                                new File(properties.getProperty("mass.messages.folder") +
                                        File.separator +
                                        fileEntry.getName()),
                                MassMessage.class
                        );
                        massMessages.add(massMessage);
                    } catch (IOException e) {
                        log.error("Ошибка при работе с файлом: {}", e.getMessage());

                        return null;
                    }
                }
            }
        } else {
            log.info("Не было ни одного массового сообщения");

            return null;
        }

        return massMessages;
    }

    public static String getLastThreeMPayments(Map<String, Long> totalAndLastThreeMPayments, SortedSet<String> keys) {
        StringBuilder resultMessage = new StringBuilder();
        for (String key : Lists.reverse(keys.stream().toList())) {
            Long value = totalAndLastThreeMPayments.get(key);
            Month month = Month.of(Math.toIntExact(Long.parseLong(key.split("\\.")[1])));
            resultMessage
                    .append(month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru-RU")))
                    .append(": ")
                    .append(value)
                    .append("₽")
                    .append("\n");
        }

        return resultMessage.toString();
    }
}