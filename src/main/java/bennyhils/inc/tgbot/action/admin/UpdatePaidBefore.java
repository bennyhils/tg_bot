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
import java.util.ArrayList;
import java.util.List;
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
                "Введите " +
                properties.getProperty("tg.admin.yes.word") +
                " (для массового обновления), логин или Id пользователя, срок продления, единицу измерения продления через пробел.\n\nНапример: " +
                "\n<code>bennyhils 1 m</code> — продлить пользователю с логином bennyhils доступ на 1 месяц," +
                "\n<code>96902655 -1 ч</code> — убавить пользователю с tgId 96902655 доступ на 1 час," +
                "\n<code>+ 2 w</code> — продлить всем пользователям доступ на 2 недели"
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

        String clientsForUpdate = parts[0];
        List<OutlineClient> updatingOutlineClients = getClientsForUpdate(clientsForUpdate);

        if (updatingOutlineClients == null) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Пользователь с tgId или логином " + clientsForUpdate + " не найден!"
            );
        }

        List<Map<String, OutlineClient>> clientsByTgId = new ArrayList<>();
        for (OutlineClient c : updatingOutlineClients) {
            clientsByTgId.add(outlineService.getClientByTgId(
                    outlineServersWithClientsMap,
                    c.getName()
            ));
        }

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
        for (Map<String, OutlineClient> clientByTgId : clientsByTgId) {

            String server = clientByTgId.keySet().stream().findFirst().orElse(null);
            OutlineClient updatingOutlineClient = clientByTgId.get(server);

            switch (time) {
                case ("H"), ("HOUR"), ("HOURS"), ("Ч"), ("Ч."), ("ЧАСОВ"), ("ЧАС"), ("ЧАСЫ") -> {
                    outlineService.updatePaidBefore(
                            server,
                            updatingOutlineClient.getPaidBefore().plus(Long.parseLong(parts[1]), ChronoUnit.HOURS),
                            updatingOutlineClient.getId().toString()
                    );
                    enableDisableClient(parts, server, updatingOutlineClient);

                }

                case ("D"), ("DAY"), ("DAYS"), ("Д"), ("ДН."), ("ДНЕЙ"), ("ДЕНЬ"), ("ДНИ") -> {
                    outlineService.updatePaidBefore(
                            server,
                            updatingOutlineClient.getPaidBefore().plus(Long.parseLong(parts[1]), ChronoUnit.DAYS),
                            updatingOutlineClient.getId().toString()
                    );
                    enableDisableClient(parts, server, updatingOutlineClient);
                }
                case ("W"), ("WEEK"), ("WEEKS"), ("Н"), ("НЕД."), ("НЕДЕЛЬ"), ("НЕДЕЛЯ"), ("НЕДЕЛИ") -> {
                    outlineService.updatePaidBefore(
                            server,
                            updatingOutlineClient.getPaidBefore().plus(Long.parseLong(parts[1]) * 7, ChronoUnit.DAYS),
                            updatingOutlineClient.getId().toString()
                    );
                    enableDisableClient(parts, server, updatingOutlineClient);
                }
                case ("M"), ("MONTH"), ("MONTHS"), ("М"), ("МЕС."), ("МЕСЯЦЕВ"), ("МЕСЯЦ"), ("МЕСЯЦА") -> {
                    outlineService.updatePaidBefore(
                            server,
                            LocalDateTime
                                    .ofInstant(updatingOutlineClient.getPaidBefore(), ZoneOffset.UTC)
                                    .plusMonths(Integer.parseInt(parts[1])).toInstant(ZoneOffset.UTC),
                            updatingOutlineClient.getId().toString()
                    );
                    enableDisableClient(parts, server, updatingOutlineClient);
                }
                default -> {
                    return new SendMessage(
                            update.getMessage().getChatId().toString(),
                            "Не удалось обновить подписку по запросу: '" + update.getMessage().getText() + "'" +
                            "\nДля обновления используйте: месяц(м, m), неделя (w, н), день (д, d), час (h, ч)"
                    );
                }
            }
        }

        List<OutlineClient> updatedOutlineClients = getClientsForUpdate(clientsForUpdate);

        if (updatedOutlineClients.size() == 0) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Не удалось обновить подписку ни одному из клиентов: " + update.getMessage().getText()
            );
        }
        if (updatedOutlineClients.size() == 1) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Обновили клиенту " + updatedOutlineClients.get(0).getName() + " время оплаты с " +
                    DataTimeUtil.getNovosibirskTimeFromInstant(updatingOutlineClients.get(0).getPaidBefore()) +
                    " до " +
                    DataTimeUtil.getNovosibirskTimeFromInstant(updatedOutlineClients.get(0).getPaidBefore())
            );

        } else {

            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    "Обновили " +
                    updatedOutlineClients.size() +
                    " клиентам время оплаты на " +
                    parts[1] +
                    " " +
                    parts[2]
            );
        }
    }

    private List<OutlineClient> getClientsForUpdate(String clientsForUpdate) {
        List<OutlineClient> updatedOutlineClients;
        if (clientsForUpdate.equals(properties.getProperty("tg.admin.yes.word"))) {
            updatedOutlineClients = outlineService.getAllServersClients(properties);
        } else {
            updatedOutlineClients = outlineService
                    .getAllServersClients(properties)
                    .stream()
                    .filter(outlineClient -> outlineClient.getName().equals(clientsForUpdate) ||
                                             outlineClient.getTgLogin().equals(clientsForUpdate)).toList();
        }
        return updatedOutlineClients;
    }

    private void enableDisableClient(String[] parts, String server, OutlineClient updatingOutlineClient) {
        if (updatingOutlineClient
                .getPaidBefore()
                .plus(Long.parseLong(parts[1]), ChronoUnit.HOURS)
                .isAfter(Instant.now())) {
            outlineService.enableClient(
                    server,
                    updatingOutlineClient.getId().toString()
            );
            log.info("Включен клиент с tgId: '{}'", updatingOutlineClient.getName());
        } else {
            outlineService.disableClient(
                    server,
                    updatingOutlineClient.getId().toString()
            );
            log.info("Выключен клиент с tgId: '{}'", updatingOutlineClient.getName());
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

    @Override
    public List<PartialBotApiMethod<Message>> sendPhoto(Update update) {

        return null;
    }
}
