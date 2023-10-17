package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.OutlineServer;
import bennyhils.inc.tgbot.vpn.OutlineService;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class UpdatePaidBefore implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public UpdatePaidBefore(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        SendMessage message = new SendMessage(
                update.getMessage().getChatId().toString(),
                "Введите логин или Id пользователя, срок продления, единицу измерения продления через пробел.\n\nНапример: <code>bennyhils 1 M</code> или <code>96902655 -1 ч</code>"
        );
        message.enableHtml(true);

        return message;
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        String message = update.getMessage().getText();
        String[] parts = message.split("[,\\s]+");

        Map<String, OutlineServer> outlineServersWithClientsMap = outlineService.getOutlineServersWithClientsMap(
                properties);

        OutlineClient updatingOutlineClient = outlineService
                .getAllServersClients(properties)
                .stream()
                .filter(outlineClient -> outlineClient.getName().equals(parts[0]) ||
                                         outlineClient.getTgLogin().equals(parts[0]))
                .findFirst()
                .orElse(null);


        if (updatingOutlineClient == null) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Пользователь с tgId или логином " + parts[0] + " не найден!"
            );
        }

        Map<String, OutlineClient> clientByTgId = outlineService.getClientByTgId(
                outlineServersWithClientsMap,
                updatingOutlineClient.getName()
        );

        String time;
        try {
            time = parts[2];
            time = time.toUpperCase();
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn("Введена неправильная команда для обновления даты подписки!");

            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Введена неправильная команда для обновления даты подписки!"
            );
        }
        switch (time) {
            case ("H"), ("HOUR"), ("HOURS"), ("Ч"), ("Ч."), ("ЧАСОВ"), ("ЧАС"), ("ЧАСЫ") -> outlineService.updatePaidBefore(
                    clientByTgId.keySet().stream().findFirst().orElse(null),
                    updatingOutlineClient.getPaidBefore().plus(Long.parseLong(parts[1]), ChronoUnit.HOURS),
                    updatingOutlineClient.getId().toString()
            );

            case ("D"), ("DAY"), ("DAYS"), ("Д"), ("ДН."), ("ДНЕЙ"), ("ДЕНЬ"), ("ДНИ") -> outlineService.updatePaidBefore(
                    clientByTgId.keySet().stream().findFirst().orElse(null),
                    updatingOutlineClient.getPaidBefore().plus(Long.parseLong(parts[1]), ChronoUnit.DAYS),
                    updatingOutlineClient.getId().toString()
            );
            case ("W"), ("WEEK"), ("WEEKS"), ("Н"), ("НЕД."), ("НЕДЕЛЬ"), ("НЕДЕЛЯ"), ("НЕДЕЛИ") -> outlineService.updatePaidBefore(
                    clientByTgId.keySet().stream().findFirst().orElse(null),
                    updatingOutlineClient.getPaidBefore().plus(Long.parseLong(parts[1]) * 7, ChronoUnit.DAYS),
                    updatingOutlineClient.getId().toString()
            );
            case ("M"), ("MONTH"), ("MONTHS"), ("М"), ("МЕС."), ("МЕСЯЦЕВ"), ("МЕСЯЦ"), ("МЕСЯЦА") -> outlineService.updatePaidBefore(
                    clientByTgId.keySet().stream().findFirst().orElse(null),
                    LocalDateTime
                            .ofInstant(updatingOutlineClient.getPaidBefore(), ZoneOffset.UTC)
                            .plusMonths(Integer.parseInt(parts[1])).toInstant(ZoneOffset.UTC),
                    updatingOutlineClient.getId().toString()
            );
            default -> {
                return new SendMessage(
                        update.getMessage().getChatId().toString(),
                        "Не удалось обновить подписку по запросу: '" + update.getMessage().getText() + "'" +
                        "\nДля обновления используйте: месяц(м, m), неделя (w, н), день (д, d), час (h, ч)"
                );

            }
        }

        OutlineClient updatedOutlineClient = outlineService
                .getAllServersClients(properties)
                .stream()
                .filter(outlineClient -> outlineClient.getName().equals(parts[0]) ||
                                         outlineClient.getTgLogin().equals(parts[0]))
                .findFirst()
                .orElse(null);

        if (updatedOutlineClient != null) {
            if (updatedOutlineClient.getPaidBefore().isAfter(Instant.now())) {
                outlineService.enableClient(
                        clientByTgId.keySet().stream().findFirst().orElse(null),
                        updatedOutlineClient.getId().toString()
                );
                log.info("Включен клиент с tgId: '{}'", updatedOutlineClient.getName());
            } else {
                outlineService.disableClient(
                        clientByTgId.keySet().stream().findFirst().orElse(null),
                        updatedOutlineClient.getId().toString()
                );
                log.info("Выключен клиент с tgId: '{}'", updatedOutlineClient.getName());
            }

            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Обновили клиенту " + updatedOutlineClient.getName() + " время оплаты с " +
                    DataTimeUtil.getNovosibirskTimeFromInstant(updatingOutlineClient.getPaidBefore()) +
                    " до " +
                    DataTimeUtil.getNovosibirskTimeFromInstant(updatedOutlineClient.getPaidBefore())
            );
        } else {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Не удалось обновить подписку: " + update.getMessage().getText()
            );
        }
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {

        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendVideo(Update update) {

        return null;
    }
}
