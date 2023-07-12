package bennyhils.inc.tgbot.action;

import bennyhils.inc.tgbot.model.Client;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.wireguard.WireGuardService;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

public class Info implements Action {

    private final Properties properties;

    WireGuardService wireGuardService = new WireGuardService();

    public Info(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        var msg = update.getMessage();
        var tgId = msg.getFrom().getId().toString();
        var chatId = msg.getChatId().toString();

        Map<String, Client> serverClient = wireGuardService.getClientByTgId(properties, tgId);

        Client existClient = null;

        if (serverClient != null) {
            existClient = serverClient.get(serverClient.keySet().stream().findFirst().isPresent() ?
                    serverClient.keySet().stream().findFirst().get() : null);
        }

        if (existClient == null) {
            return new SendMessage(chatId, """
                    У вас нету нашего VPN.

                    Чтобы получить его бесплатно на 2 дня, нажмите /buy""");

        } else {

            Instant now = Instant.now();

            Instant paidBefore = existClient.getPaidBefore();

            if (now.isAfter(paidBefore)) {
                return new SendMessage(
                        chatId,
                        "У вас был наш VPN, но срок его действия закончился в " +
                        DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) + "." +
                        "\n" +
                        "\n" +
                        "Чтобы оплатить его нажмите /buy"
                );
            } else {
                return new SendMessage(chatId, "У вас есть доступ до " +
                                               DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) +
                                               "." +
                                               "\n" +
                                               "\n" +
                                               "Вы можете продлить доступ заранее, не дожидаясь отключения, для этого нажмите /buy");
            }
        }
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        return new SendMessage(update.getMessage().getChatId().toString(), "/start");
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(
            Update update
    ) {
        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {
        return null;
    }
}
