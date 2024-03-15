package bennyhils.inc.tgbot.action;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.model.Referral;
import bennyhils.inc.tgbot.util.FileEngine;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public record InfoAction(List<String> actions, Properties properties) implements Action {
    private static final OutlineService outlineService = new OutlineService();

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        long referrerTgId = 0;
        String referrerName = String.valueOf(referrerTgId);
        if (update.getMessage().getText().split("[,\\s]+").length > 1) {
            referrerTgId = Long.parseLong(update.getMessage().getText().split("[,\\s]+")[1]);
            referrerName = String.valueOf(referrerTgId);
            Map<String, OutlineServer> allClients = outlineService.getOutlineServersWithClientsMap(properties);
            Map<String, OutlineClient> clientByTgId = outlineService.getClientByTgId(allClients, String.valueOf(referrerTgId));
            OutlineClient referrer = clientByTgId.get(clientByTgId.keySet().stream().findFirst().orElse(null));
            if (referrer != null) {
                referrerName = referrer.getNameForMessage(referrer);
            }
        }
        var fromId = update.getMessage().getFrom().getId().toString();
        var out = new StringBuilder();
        out.append("Для работы с ботом используйте следующие команды:").append("\n");
        for (String action : actions) {
            out.append(action).append("\n");
        }

        if (referrerTgId != 0L && update.getMessage().getFrom().getId() != referrerTgId) {
            List<Referral> referrals = FileEngine.getReferrals(properties);
            Referral referral = referrals.stream().filter(r -> r.getReferralId() == update.getMessage().getFrom().getId()).findFirst().orElse(null);
            if (referral == null) {
                FileEngine.writeReferralToFile(new Referral(update.getMessage().getFrom().getId(), referrerTgId, 0, false, null), properties);
                String referralName = String.valueOf(update.getMessage().getFrom().getId());
                if (update.getMessage().getFrom().getLastName() != null)
                    referralName = update.getMessage().getFrom().getFirstName() + " " + update.getMessage().getFrom().getLastName();
                if (update.getMessage().getFrom().getUserName() != null) {
                    referralName = "@" + update.getMessage().getFrom().getUserName();
                }

                return List.of(
                        new SendMessage(fromId, """
                                Поздравляем, вы зарегистрировались в программе лояльности как гость.
                                                    
                                Вас пригласил пользователь %s, вы так же можете приглашать друзей и получать бесплатно время подписки, для этого нажмите /loyalty
                                                    
                                """.formatted(referrerName) + out),
                        new SendMessage(String.valueOf(referrerTgId), """
                                Пользователь %s, приглашенный вами, зарегистрировался в программе лояльности.
                                                            
                                Когда он оплатит подписку первый раз, мы вышлем вам уведомление и подарим столько месяцев подписки, сколько он оплатил.
                                                            
                                Благодарим за то, что выбираете нас ❤️""".formatted(referralName)));
            }

            if (!referral.isActivated()) {

                return List.of(new SendMessage(fromId, """
                        Вы уже зарегистрированы в программе лояльности. Когда оплатите подписку первый раз, мы подарим столько же месяцев подписки пользователю %s, пригласившему вас.
                                                    
                        """.formatted(referrerName) + out));
            } else {

                return List.of(new SendMessage(fromId, """
                        Вы зарегистрированы в программе лояльности как гость и уже совершили свой первый платеж на %s мес. За это мы подарили %s мес. подписки пользователю %s.
                                                    
                        """.formatted(referral.getFirstPaymentMonthCount(), referral.getFirstPaymentMonthCount(), referrerName) + out));
            }
        } else {

            return List.of(new SendMessage(fromId, out.toString()));
        }
    }

    @Override
    public List<BotApiMethod<?>> callback(Update update) {

        return null;
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
