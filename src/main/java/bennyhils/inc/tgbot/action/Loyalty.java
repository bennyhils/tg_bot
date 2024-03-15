package bennyhils.inc.tgbot.action;

import bennyhils.inc.tgbot.model.Payment;
import bennyhils.inc.tgbot.model.Referral;
import bennyhils.inc.tgbot.util.FileEngine;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
public class Loyalty implements Action {

    public final static String TOTAL_PAYMENTS_KEY = "totalPaymentsKey";

    private final Properties properties;

    public Loyalty(
            Properties properties
    ) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {

            return null;
        }
        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();
        List<Referral> clientReferrals = FileEngine.getClientReferrals(properties, Long.parseLong(tgId));
        String refStat = null;
        if (!clientReferrals.isEmpty()) {
            long activatedClients = clientReferrals.stream().filter(Referral::isActivated).count();
            int mountsSum = clientReferrals.stream().map(Referral::getFirstPaymentMonthCount)
                    .reduce(0, Integer::sum);
            refStat = """                     
                                      
                    
                    По вашей личной ссылке в программе лояльности:
                    - %s чел. зарегистрировалось,
                    - %s раз оплатили первый раз,
                    - %s мес. оплатили первый раз (столько мес. вы получили бесплатно)
                        """.formatted(clientReferrals.size(), activatedClients, mountsSum);

        }
        String message = "Если ваш друг зарегистрируется по этой ссылке и оплатит подписку, вы получите бесплатный доступ к VPN на срок, равный его первой подписке (1, 3 или 6 месяцев) \n \nhttps://t.me/" +
                properties.getProperty("tg.username") +
                "?start=" +
                tgId;
        if (refStat != null) {
            message = message + refStat;
        }

        return List.of(new SendMessage(tgId,
                message));
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText() || !update.getMessage().getText().equals(properties.getProperty("tg.admin.yes.word"))) {

            return null;
        }
        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();
        List<Referral> clientReferrals = FileEngine.getClientReferrals(properties, Long.parseLong(tgId));
        List<Payment> allPayments = FileEngine.getAllPayments(properties);
        Map<Referral, List<Payment>> paymantsReferralMap = new HashMap<>();

        if (clientReferrals.isEmpty()) {

            return List.of(new SendMessage(tgId, "Никто не зарегистрировался в программе лояльности по вашей ссылке"));
        }
        if (allPayments == null || allPayments.isEmpty()) {

            return List.of(new SendMessage(tgId, """
                     По вашей ссылке в программе лояльности зарегистрировалось %s чел., но никто еще не платил
                    """.formatted(clientReferrals.size())));
        }
        for (Referral r : clientReferrals) {
            List<Payment> payments = allPayments.stream().filter(p -> p.getTgId().equals(String.valueOf(r.getReferralId())) && r.getActivatingTime() != null && p.getPaymentEpochMilli() >= r.getActivatingTime().minus(1, ChronoUnit.MINUTES).toEpochMilli()).toList();
            if (!payments.isEmpty()) {
                paymantsReferralMap.put(r, payments);
            }
        }
        List<Payment> payments = new ArrayList<>();
        for (Referral r : paymantsReferralMap.keySet()) {
            payments.addAll(paymantsReferralMap.get(r));
        }
        Map<String, Long> totalAndLastThreeMPayments = FileEngine.getTotalAndLastThreeMPayments(null, payments);
        if (totalAndLastThreeMPayments == null || payments.isEmpty()) {

            return List.of(new SendMessage(tgId, """
                    По вашей ссылке в программе лояльности зарегистрировалось %s чел., но никто еще не платил
                    """.formatted(clientReferrals.size())));
        }
        SortedSet<String> keys = new TreeSet<>(totalAndLastThreeMPayments.keySet());
        keys.remove(TOTAL_PAYMENTS_KEY);
        String resultMessage = "Ваши приглашенные клиенты оплатили за последние 3 месяца: \n\n" +
                FileEngine.getLastThreeMPayments(totalAndLastThreeMPayments, keys);
        SendMessage sendMessage = new SendMessage(update.getMessage().getChatId().toString(), resultMessage);
        sendMessage.enableHtml(true);

        return List.of(sendMessage);
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {

        return null;
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
