package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.BotMenu;
import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.MassMessage;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import bennyhils.inc.tgbot.util.FileEngine;
import bennyhils.inc.tgbot.vpn.OutlineService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static bennyhils.inc.tgbot.util.JsonParserUtil.OBJECT_MAPPER;

@Slf4j
public class MassMessages implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public MassMessages(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {
        List<MassMessage> allMassMessages = FileEngine.getAllMassMessages(properties);
        if (allMassMessages == null || allMassMessages.isEmpty()) {
            return new SendMessage(
                    update.getMessage().getChatId().toString(),
                    """
                            У вас нету никаких массовых сообщений.
                                                        
                            Сконструируйте сообщения для отправки так: введите текст сообщения (минимум 3 слова) и приложите картинку при необходимости
                            """);
        } else {
            Collections.sort(allMassMessages);
            StringBuilder massMessagesSB = new StringBuilder();
            massMessagesSB.append("\n<code>id, Текс сообщения, Картинка, Последняя отправка, Получили/Отправлялось чел. \n\n");
            for (MassMessage mm : Lists.reverse(allMassMessages.subList(Math.max((allMassMessages.size() - 3), 0), allMassMessages.size()))) {
                massMessagesSB.append(mm.getId());
                massMessagesSB.append(", \"");
                massMessagesSB.append(mm.getMessage());
                massMessagesSB.append("\", ");
                massMessagesSB.append(mm.getPicId() == 0L ? "Нету" : "Есть");
                massMessagesSB.append(", ");
                massMessagesSB.append(mm.getSendingTime() == null ? "Не отправлялось" : DataTimeUtil.getFileNameForCheck(mm.getSendingTime()));
                massMessagesSB.append(", ");
                massMessagesSB.append(mm.getDeliveredTo()).append("/").append(mm.getSentTo());
                massMessagesSB.append("\n");
            }
            SendMessage sendMessage = new SendMessage(
                    update.getMessage().getChatId().toString(),
                    """
                            Последние массовые сообщения:
                            %s</code>
                            У вас есть 3 варианта дальнейшего взаимодействия с этой кнопкой:
                                                    
                            1. Введите подтверждение, чтобы получить файл со всеми массовыми сообщениями и статистикой отправки по ним.
                                                   
                            2. Введите (номер сообщения) и (ids или логины клиентов), которым вы хотите его отправить массовое сообщение.
                            Формат такой:
                            <code>7  ["bennyhils",1332441821,"@romanchovgun"]</code> — отправить сообщение с номером 7 трем указанным клиентам [Квадратные скобочки и кавычки для строк — обязательны. Используется JSON формат. Не используйте пробелы после запятых],
                            <code>22 +</code> — отправить сообщение 22 всем клиентам бота.
                                                    
                            3. Сконструируйте сообщения для отправки так: введите текст сообщения (минимум 3 слова) и приложите картинку при необходимости
                            """.formatted(massMessagesSB));
            sendMessage.enableHtml(true);

            return sendMessage;
        }
    }

    @Override
    public BotApiMethod<?> callback(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String[] parts = update.getMessage().getText().split("\\s+");
            int l = parts.length;
            switch (l) {
                case (1): {
                    return null;
                }
                case (2): {
                    int mmId;
                    try {
                        mmId = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {

                        return new SendMessage(update.getMessage().getChatId().toString(), "Id сообщения должно быть цифрой, а вы ввели: '" + parts[0] + "'");
                    }

                    List<MassMessage> allMassMessages = FileEngine.getAllMassMessages(properties);
                    MassMessage massMessage;
                    if (allMassMessages != null) {
                        massMessage = allMassMessages.stream().filter(m -> m.getId() == mmId).findFirst().orElse(null);
                    } else {

                        return new SendMessage(update.getMessage().getChatId().toString(), "Не найдено сообщение с id: '" + parts[0] + "'");
                    }

                    List<String> recipientsRaw = new ArrayList<>();

                    if (!parts[1].equals(properties.getProperty("tg.admin.yes.word"))) {

                        try {
                            recipientsRaw = OBJECT_MAPPER.readValue(
                                    parts[1].toUpperCase().replace("@", ""),
                                    new TypeReference<>() {
                                    }
                            );
                        } catch (JsonProcessingException e) {
                            String text = """
                                    Неправильный формат получателей. Доступны два формата:
                                    1. <code>2 +</code> — отправить сообщение с id = 2 всем клиентам.
                                    2. <code>5 ["bennyhils",1332441821,"@romanchovgun"]</code> — отправить сообщение с id = 5 трем клиентам. Не используйте пробелы для разделения""";
                            SendMessage sendMessage = new SendMessage(update.getMessage().getChatId().toString(),
                                    text);
                            sendMessage.enableHtml(true);
                            return sendMessage;
                        }
                    }

                    if (massMessage != null) {

                        return new SendMessage(update.getMessage().getChatId().toString(),
                                """
                                        Массовое сообщение '%s' будет отправлено клиентам '%s'
                                        """.formatted(massMessage.getMessage(), parts[1].equals(properties.getProperty("tg.admin.yes.word")) ? "Все" : recipientsRaw.stream().map(String::toString).collect(Collectors.joining(","))));
                    } else {

                        return new SendMessage(update.getMessage().getChatId().toString(),
                                """
                                        Массовое сообщение не найдено""");
                    }

                }
            }
            if (l >= 3) {
                long id = BotMenu.getNextMMid(properties);
                FileEngine.writeMassMessageToFile(
                        id,
                        null,
                        update.getMessage().getText(),
                        0L,
                        0L,
                        0L,
                        properties

                );

                return new SendMessage(update.getMessage().getChatId().toString(), "Сохранено массовое сообщение без картинки: " +
                        update.getMessage().getText());

            }
        }

        return null;
    }

    @Override
    public PartialBotApiMethod<Message> sendDocument(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals(properties.getProperty("tg.admin.yes.word"))) {
            InputStream targetStream;
            List<MassMessage> massMessages = FileEngine.getAllMassMessages(properties);
            if (massMessages != null) {
                massMessages.sort(Comparator.comparing(MassMessage::getId));
                massMessages = Lists.reverse(massMessages);
                StringBuilder result = new StringBuilder();
                result.append("Массовые сообщения: ").append(" \n");
                result.append(
                        "    #, Текст, Есть картинка, Время отправки, Получили/Отправлялось, чел. \n");

                for (MassMessage m : massMessages) {

                    result
                            .append("\n    ")
                            .append(m.getId())
                            .append(", \"")
                            .append(m.getMessage())
                            .append("\", ")
                            .append(m.getPicId() == 0 ? "Нету" : "Есть")
                            .append(", ")
                            .append(m.getSendingTime() == null ? "Не отправлялось" : DataTimeUtil
                                    .getFileNameForCheck(m.getSendingTime()))
                            .append(", ")
                            .append(m.getSentTo())
                            .append(", ")
                            .append(m.getDeliveredTo())
                            .append(", ")
                            .append(m.getDeliveredTo()).append("/").append(m.getSentTo())
                            .append("\n");
                }
                targetStream = new ByteArrayInputStream(result.toString().getBytes());
                SendDocument sendDocument = new SendDocument(
                        update.getMessage().getChatId().toString(),
                        new InputFile(targetStream, "Все массовые сообщения.txt")
                );
                sendDocument.setCaption("Массовые сообщения");

                return sendDocument;
            } else {

                return null;
            }
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
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().split("\\s+").length == 2) {
            String[] parts = update.getMessage().getText().split("\\s+");
            int mmId;
            try {
                mmId = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return null;
            }
            List<MassMessage> allMassMessages = FileEngine.getAllMassMessages(properties);
            int finalMmId = mmId;
            MassMessage massMessage = null;
            if (allMassMessages != null) {
                massMessage = allMassMessages.stream().filter(m -> m.getId() == finalMmId).findFirst().orElse(null);
            }
            if (massMessage == null) {
                return null;
            }

            List<String> recipientsRaw = new ArrayList<>();
            List<OutlineClient> allServersClients = outlineService.getAllServersClients(properties);
            List<String> recipients = new ArrayList<>();
            if (parts[1].equals(properties.getProperty("tg.admin.yes.word"))) {
                recipients = allServersClients.stream().map(OutlineClient::getName).toList();
            } else {
                try {
                    recipientsRaw = OBJECT_MAPPER.readValue(
                            parts[1].toUpperCase().replace("@", ""),
                            new TypeReference<>() {
                            }
                    );
                } catch (JsonProcessingException e) {

                    return null;
                }
            }

            for (OutlineClient c : allServersClients) {
                if (recipientsRaw.contains(c.getTgLogin().toUpperCase()) || recipientsRaw.contains(c.getName())) {
                    recipients.add(c.getName());
                }
            }

            Map<Long, List<PartialBotApiMethod<Message>>> mm = new HashMap<>();

            List<PartialBotApiMethod<Message>> mmById = new ArrayList<>();

            for (String r : recipients) {
                mmById.add(getSendPhoto(r, massMessage, properties));
            }
            mm.put((long) mmId, mmById);

            return mm;
        } else {

            return null;
        }
    }

    private static SendPhoto getSendPhoto(String idSendTo, MassMessage massMessage, Properties properties) {
        SendPhoto photo;
        if (massMessage == null) {

            return null;
        } else {
            photo = new SendPhoto(
                    idSendTo,
                    new InputFile(new File(properties.getProperty("mass.messages.folder") +
                            java.io.File.separator +
                            properties.getProperty("mass.messages.photos.folder") +
                            java.io.File.separator +
                            massMessage.getPicId() +
                            properties.getProperty("mass.messages.photo.png")))
            );
            photo.setCaption(massMessage.getMessage());
            photo.setParseMode("HTML");
        }

        return photo;
    }
}
