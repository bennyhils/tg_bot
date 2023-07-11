package bennyhils.inc.tgbot.shedulers;

import bennyhils.inc.tgbot.BotMenu;
import bennyhils.inc.tgbot.model.Client;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.wireguard.WireGuardClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

@Slf4j
public class DisableScheduler extends TimerTask {

    public String url;
    public String session;
    public BotMenu botMenu;

    public DisableScheduler(String url, String session, BotMenu botMenu) {
        this.url = url;
        this.session = session;
        this.botMenu = botMenu;
    }

    WireGuardClient wireGuardClient = new WireGuardClient();

    public void run() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();

            List<Client> allClients = objectMapper.readValue(
                    wireGuardClient.getClients(url, session).body(), new TypeReference<>() {
                    });

            LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

            for (Client client : allClients) {
                if (client.getPaidBefore().isBefore(now)
                    && client.isEnabled()) {
                    wireGuardClient.disableClient(client.getId(), url, session);
                    log.info("Выключен за неуплату: " +
                             (client.getTgLogin() == null || client.getTgLogin().equals("null") ?
                                     client.getTgId() : client.getTgLogin()));
                    botMenu.sendMsg(new SendMessage(
                            client.getTgId(),
                            "Мы выключили вам VPN, потому что у вас закончился доступ в " + client.getPaidBefore().format(DataTimeUtil.DATE_TIME_FORMATTER) + "" +
                            "\n" +
                            "\n" +
                            "Возвращайтесь скорее!" +
                            "\n" +
                            "\n" +
                            "Чтобы продлить доступ нажмите /buy"));
                }
            }

        } catch (Exception e) {
            log.error("Ошибка запуска потока " + Arrays.toString(e.getStackTrace()));
        }
    }
}