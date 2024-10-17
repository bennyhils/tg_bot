package bennyhils.inc.tgbot.shedulers;

import bennyhils.inc.tgbot.BotMenu;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

@Slf4j
public class DisableScheduler extends TimerTask {
    public Properties properties;
    public BotMenu botMenu;

    public DisableScheduler(Properties properties, BotMenu botMenu) {
        this.properties = properties;
        this.botMenu = botMenu;
    }

    OutlineService outlineService = new OutlineService();

    public void run() {
        try {

            Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                    properties);

            Instant now = Instant.now();

            for (String s : outlineServersWithClientsMap.keySet()) {
                for (OutlineClient c : outlineServersWithClientsMap.get(s).getClients()) {
                    if (c.getPaidBefore() != null && c.getPaidBefore().isBefore(now) && c.getDataLimit() == null) {
                        outlineService.disableClient(s, c.getId().toString());
                        log.info("Выключен за неуплату: " +
                                 (c.getTgLogin() == null || c.getTgLogin().equals("null") ?
                                         c.getName() : c.getTgLogin()));
                        botMenu.sendMsg(new SendMessage(
                                c.getName(),
                                "Мы выключили вам VPN, потому что у вас закончился доступ в " +
                                DataTimeUtil.getNovosibirskTimeFromInstant(c.getPaidBefore()) +
                                "\n" +
                                "\n" +
                                "Возвращайтесь скорее!" +
                                "\n" +
                                "\n" +
                                "Чтобы продлить доступ нажмите /buy"
                        ));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Ошибка запуска потока " + Arrays.toString(e.getStackTrace()));
        }
    }
}