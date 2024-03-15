package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.Payment;
import bennyhils.inc.tgbot.model.Referral;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.util.FileEngine;
import bennyhils.inc.tgbot.vpn.OutlineService;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GetLoyaltyStatistics implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public GetLoyaltyStatistics(Properties properties) {
        this.properties = properties;
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {

            return null;
        }
        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();
        List<Referral> allReferrals = FileEngine.getReferrals(properties);
        if (!allReferrals.isEmpty()) {
            long activatedClients = allReferrals.stream().filter(Referral::isActivated).count();
            int mountsSum = allReferrals.stream().map(Referral::getFirstPaymentMonthCount)
                    .reduce(0, Integer::sum);
            String refStat = """
                                        
                                        
                    Всего в программе лояльности:
                    - %s чел. зарегистрировалось,
                    - %s чел. получили ключ и оплатили первый раз,,
                    - %s мес. оплатили первый раз (столько мес. мы раздали бесплатно)
                        """.formatted(allReferrals.size(), activatedClients, mountsSum);

            String endMsg = "\nВведите логин или Id пользователя, статистику по программе лояльности которого хотите посмотреть. Или выслать полную статистику файлом?";

            return List.of(new SendMessage(tgId,
                    refStat + endMsg));
        } else {

            return List.of(new SendMessage(tgId,
                    "Никто не зарегистрировался в программе лояльности"));
        }
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {

            return null;
        }
        if (update.getMessage().getText().startsWith("/") || update.getMessage().getText().equals(properties.getProperty("tg.admin.yes.word"))) {

            return null;
        }
        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();
        String referrerTgIdoTgId = msg.getText();

        List<OutlineClient> allServersClients = outlineService.getAllServersClients(properties);
        Set<OutlineClient> findClients = allServersClients
                .stream()
                .filter(c -> c.getName() != null &&
                        c.getTgLogin() != null &&
                        c.getTgFirst() != null &&
                        c.getTgLast() != null)
                .filter(
                        c -> (c.getName().equals(referrerTgIdoTgId)) ||
                                c.getTgLogin().equalsIgnoreCase(referrerTgIdoTgId) ||
                                c.getTgLogin().equalsIgnoreCase(referrerTgIdoTgId.replace("@", "")) ||
                                c.getTgLogin().toUpperCase().contains(referrerTgIdoTgId.toUpperCase()) ||
                                c.getTgFirst().equalsIgnoreCase(referrerTgIdoTgId) ||
                                c.getTgFirst().toUpperCase().contains(referrerTgIdoTgId.toUpperCase()) ||
                                c.getTgLast().equalsIgnoreCase(referrerTgIdoTgId) ||
                                c.getTgLast().toUpperCase().contains(referrerTgIdoTgId.toUpperCase()))
                .collect(Collectors.toSet());
        if (findClients.isEmpty()) {

            return List.of(new SendMessage(tgId, "Никого из клиентов не нашлось. \n\nИскали по: " + referrerTgIdoTgId));
        }
        if (findClients.size() >= 2) {

            return List.of(new SendMessage(tgId, "Нашлось больше одного клиента, по которому вы хотите посмотреть статистику. Уточните параметры поиска и повторите!"));
        }
        OutlineClient outlineClient = findClients.stream().findFirst().get();
        List<Referral> clientReferrals = FileEngine.getClientReferrals(properties, Long.parseLong(outlineClient.getName()));
        List<Payment> allPayments = FileEngine.getAllPayments(properties);
        Map<Referral, List<Payment>> paymantsReferralMap = new HashMap<>();
        if (clientReferrals.isEmpty()) {

            return List.of(new SendMessage(tgId, "Никто не зарегистрировался в программе лояльности от клиента " + outlineClient.getNameForMessage(outlineClient)));
        }
        if (allPayments == null || allPayments.isEmpty()) {

            return List.of(new SendMessage(tgId, """
                    По ссылке клиента %s в программе лояльности зарегистрировалось %s чел., но никто еще не платил
                    """.formatted(outlineClient.getNameForMessage(outlineClient), clientReferrals.size())));
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
                    По ссылке клиента %s в программе лояльности зарегистрировалось %s чел., но никто еще не платил
                    """.formatted(outlineClient.getNameForMessage(outlineClient), clientReferrals.size())));

        }
        SortedSet<String> keys = new TreeSet<>(totalAndLastThreeMPayments.keySet());
        keys.remove(FileEngine.TOTAL_PAYMENTS_KEY);
        String resultMessage = "Пользователи, приглашенные клиентом " + outlineClient.getNameForMessage(outlineClient) + ", оплатили за последние 3 месяца: \n\n" +
                FileEngine.getLastThreeMPayments(totalAndLastThreeMPayments, keys) +
                "\nОплатили всего с момента регистрации в программе: " + totalAndLastThreeMPayments.get(FileEngine.TOTAL_PAYMENTS_KEY) + "₽\n";
        SendMessage sendMessage = new SendMessage(update.getMessage().getChatId().toString(), resultMessage);
        sendMessage.enableHtml(true);

        return List.of(sendMessage);
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
            List<Referral> referrals = FileEngine.getReferrals(properties);
            if (referrals.isEmpty()) {

                return null;
            }
            referrals.sort(Comparator.comparing(Referral::getReferrerId));
            StringBuilder result = new StringBuilder();
            result.append("Программа лояльности: ").append(" \n");
            List<OutlineClient> allServersClients = outlineService.getAllServersClients(properties);
            for (Referral r : referrals) {
                result.append(
                        "    Клиент ");
                OutlineClient referral = allServersClients
                        .stream()
                        .filter(c -> c.getName().equals(String.valueOf(r.getReferralId())))
                        .findFirst()
                        .orElse(new OutlineClient());
                OutlineClient referrer = allServersClients
                        .stream()
                        .filter(c -> c.getName().equals(String.valueOf(r.getReferrerId())))
                        .findFirst()
                        .orElse(new OutlineClient());
                result
                        .append(referrer.getNameForMessage(referrer) == null ? r.getReferrerId() : referrer.getNameForMessage(referrer))
                        .append(" пригласил клиента ")
                        .append(referral.getNameForMessage(referral) == null ? r.getReferralId() : referral.getNameForMessage(referral));
                if (r.getFirstPaymentMonthCount() == 0) {
                    result
                            .append(" и приглашенный еще не платил \n");

                } else {
                    result
                            .append(" и приглашенный оплатил первый раз ")
                            .append(DataTimeUtil.getNovosibirskTimeFromInstant(r.getActivatingTime()))
                            .append(" на ")
                            .append(r.getFirstPaymentMonthCount())
                            .append(" мес.")
                            .append("\n");
                }
            }
            targetStream = new ByteArrayInputStream(result.toString().getBytes());
            SendDocument sendDocument = new SendDocument(
                    update.getMessage().getChatId().toString(),
                    new InputFile(targetStream, "Программа лояльности.txt")
            );
            sendDocument.setCaption("Программа лояльности");

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
