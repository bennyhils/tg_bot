package bennyhils.inc.tgbot;

import bennyhils.inc.tgbot.action.Buy;
import bennyhils.inc.tgbot.action.admin.DeleteClient;
import bennyhils.inc.tgbot.action.admin.GetClients;
import bennyhils.inc.tgbot.action.admin.FindClient;
import bennyhils.inc.tgbot.action.Help;
import bennyhils.inc.tgbot.action.InfoAction;
import bennyhils.inc.tgbot.action.Instruction;
import bennyhils.inc.tgbot.action.Info;
import bennyhils.inc.tgbot.action.admin.GetPayments;
import bennyhils.inc.tgbot.action.admin.UpdatePaidBefore;
import bennyhils.inc.tgbot.shedulers.DisableScheduler;
import bennyhils.inc.tgbot.shedulers.RemindPaymentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Bot {
    public static void main(String[] args) throws TelegramApiException, IOException {

        var tg = new TelegramBotsApi(DefaultBotSession.class);
        var properties = new Properties();
        try (var app = Bot.class.getClassLoader().getResourceAsStream("app.properties")) {
            properties.load(app);
        }

        var usersActions = new HashMap<>(Map.of(
                "/start", new InfoAction(
                        List.of(
                                "/start — описание команд бота",
                                "/buy — получение, покупка или продление VPN",
                                "/info — состояние вашего VPN",
                                "/instruction — инструкция для настройки, ключ для подключения",
                                "/help — поддержка"
                        )
                ),
                "/buy", new Buy(properties),
                "/info", new Info(properties),
                "/instruction", new Instruction(properties),
                "/help", new Help(properties.getProperty("tg.support.user.name"))

        ));

        var adminsActions = Map.of(
                "/admin", new InfoAction(List.of(
                        "/c — показать клиентов",
                        "/p — показать оплаты",
                        "/f — найти клиентов по tgId, логину, имени или фамилии",
                        "/u — добавить или убавить время подписки",
                        "/d — удалить ключ клиента"
                )),
                "/c", new GetClients(properties),
                "/p", new GetPayments(properties),
                "/f", new FindClient(properties),
                "/u", new UpdatePaidBefore(properties),
                "/d", new DeleteClient(properties)
        );


        runDisableSchedulerTask(
                properties,
                new BotMenu(
                        usersActions,
                        adminsActions,
                        properties
                )
        );

        runRemindPaymentSchedulerTask(
                properties,
                new BotMenu(
                        usersActions,
                        adminsActions,
                        properties
                )
        );


        tg.registerBot(new BotMenu(
                usersActions,
                adminsActions,
                properties
        ));
    }

    static private void runDisableSchedulerTask(
            Properties properties,
            BotMenu botMenu
    ) {
        long disableStartInHours = Long.parseLong(properties.getProperty("disable.start.in.hours"));
        long disablePeriodInHours = Long.parseLong(properties.getProperty("disable.period.in.hours"));

        log.info("Запущен планировщик отключения за неуплату");
        Timer time = new Timer();
        time.schedule(
                new DisableScheduler(properties, botMenu),
                TimeUnit.HOURS.toMillis(disableStartInHours),
                TimeUnit.HOURS.toMillis(disablePeriodInHours)
        );
    }

    static private void runRemindPaymentSchedulerTask(Properties properties, BotMenu botMenu) {
        long remindStartInHours = Long.parseLong(properties.getProperty("remind.start.in.hours"));
        long remindPeriodInDays = Long.parseLong(properties.getProperty("remind.period.in.days"));

        log.info("Запущен напоминатель заблаговременной оплаты");
        Timer time = new Timer();
        time.schedule(
                new RemindPaymentScheduler(properties, botMenu),
                TimeUnit.HOURS.toMillis(remindStartInHours),
                TimeUnit.DAYS.toMillis(remindPeriodInDays)
        );
    }
}
