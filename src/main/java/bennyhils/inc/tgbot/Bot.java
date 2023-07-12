package bennyhils.inc.tgbot;

import bennyhils.inc.tgbot.action.Buy;
import bennyhils.inc.tgbot.action.Help;
import bennyhils.inc.tgbot.action.InfoAction;
import bennyhils.inc.tgbot.action.Instruction;
import bennyhils.inc.tgbot.action.Info;
import bennyhils.inc.tgbot.model.Server;
import bennyhils.inc.tgbot.shedulers.DisableScheduler;
import bennyhils.inc.tgbot.shedulers.RemindPaymentScheduler;
import bennyhils.inc.tgbot.wireguard.WireGuardService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Bot {
    public static void main(String[] args) throws TelegramApiException, IOException {

        var tg = new TelegramBotsApi(DefaultBotSession.class);
        var config = new Properties();
        try (var app = Bot.class.getClassLoader().getResourceAsStream("app.properties")) {
            config.load(app);
        }

        WireGuardService wireGuardService = new WireGuardService();

        Map<String, Server> servers = wireGuardService.getServersClients(config);


        var actions = Map.of(
                "/start", new InfoAction(
                        List.of(
                                "/start — описание команд бота",
                                "/buy — получение, покупка или продление VPN",
                                "/info — состояние вашего VPN",
                                "/instruction — инструкция для настройки VPN на телефоне, получение файла для подключения",
                                "/help — поддержка"
                        )
                ),
                "/buy", new Buy(servers, config),
                "/info", new Info(config),
                "/instruction", new Instruction(servers, config),
                "/help", new Help(config.getProperty("tg.support.user.name"))
        );

        for (String server : servers.keySet()) {
            //Проверяем выключаем доступ неоплатившим
            runDisableSchedulerTask(
                    server,
                    servers.get(server).getCookie(),
                    new BotMenu(
                            actions,
                            config.getProperty("tg.username"),
                            config.getProperty("tg.token"),
                            config.getProperty("provider.token"),
                            config.getProperty("mail")
                    ), config
            );

            runRemindPaymentSchedulerTask(server, servers.get(server).getCookie(), new BotMenu(
                    actions,
                    config.getProperty("tg.username"),
                    config.getProperty("tg.token"),
                    config.getProperty("provider.token"),
                    config.getProperty("mail")
            ), config);
        }

        tg.registerBot(new BotMenu(
                actions,
                config.getProperty("tg.username"),
                config.getProperty("tg.token"),
                config.getProperty("provider.token"),
                config.getProperty("mail")
        ));
    }

    static private void runDisableSchedulerTask(String url, String session, BotMenu botMenu, Properties config) {
        long disableStartInMs = 0;
        long disablePeriodInHours = Long.parseLong(config.getProperty("disable.period.in.hours"));

        log.info("Запущен планировщик отключения за неуплату");
        Timer time = new Timer();
        time.schedule(
                new DisableScheduler(url, session, botMenu),
                disableStartInMs,
                TimeUnit.HOURS.toMillis(disablePeriodInHours)
        );
    }

    static private void runRemindPaymentSchedulerTask(String url, String session, BotMenu botMenu, Properties config) {
        long remindStartInHours = Long.parseLong(config.getProperty("remind.start.in.hours"));
        long remindPeriodInDays = Long.parseLong(config.getProperty("remind.period.in.days"));

        log.info("Запущен напоминатель заблаговременной оплаты");
        Timer time = new Timer();
        time.schedule(
                new RemindPaymentScheduler(url, session, botMenu),
                TimeUnit.HOURS.toMillis(remindStartInHours),
                TimeUnit.DAYS.toMillis(remindPeriodInDays)
        );
    }
}
