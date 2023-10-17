package bennyhils.inc.tgbot.shedulers;

import bennyhils.inc.tgbot.BotMenu;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

@Slf4j
public class RemindPaymentScheduler extends TimerTask {

    public Properties properties;
    public BotMenu botMenu;

    public RemindPaymentScheduler(Properties properties, BotMenu botMenu) {
        this.properties = properties;
        this.botMenu = botMenu;
    }

    OutlineService outlineService = new OutlineService();

    public void run() {
        try {

            Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                    properties);


            Instant now = Instant.now();
            Instant nowPlusDay = now.plus(1, ChronoUnit.DAYS);

            for (String s : outlineServersWithClientsMap.keySet()) {
                for (OutlineClient c : outlineServersWithClientsMap.get(s).getClients()) {
                    if (c.getPaidBefore() != null &&
                        c.getPaidBefore().isAfter(now) &&
                        c.getPaidBefore().isBefore(nowPlusDay)
                        &&
                        c.getDataLimit() != null) {
                        botMenu.sendMsg(
                                new SendMessage(
                                        c.getName(),
                                        """
                                                Срок действия VPN заканчивается совсем скоро, в %s
                                                                                   
                                                                                   
                                                Вы можете продлить доступ заранее, не дожидаясь отключения, для этого нажмите /buy"""
                                                .formatted(DataTimeUtil.getNovosibirskTimeFromInstant(c.getPaidBefore()))
                                ));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка запуска потока " + Arrays.toString(e.getStackTrace()));
        }
    }
}