package bennyhils.inc.tgbot.action.admin;

import bennyhils.inc.tgbot.action.Action;
import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.vpn.OutlineService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class MassMessagesSender implements Action {

    private final Properties properties;

    private final OutlineService outlineService = new OutlineService();

    public MassMessagesSender(Properties properties) {
        this.properties = properties;
    }

    @Override
    public BotApiMethod<?> handle(Update update) {

        return new SendMessage(
                update.getMessage().getChatId().toString(),
                "mm"
        );

    }

    @Override
    public BotApiMethod<?> callback(Update update) {

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
    public List<PartialBotApiMethod<Message>> sendPhoto(Update update) {
        List<OutlineClient> outlineClients = outlineService.getAllServersClients(properties);

        List<PartialBotApiMethod<Message>> messages = new ArrayList<>();

        for (String c : outlineClients.stream().map(OutlineClient::getName).collect(Collectors.toSet())
        ) {

            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is;
            is = classloader.getResourceAsStream("Migration1.png");

            String name = "друг";

            OutlineClient outlineClient = outlineClients
                    .stream()
                    .filter(oc -> oc.getName().equals(c))
                    .findFirst()
                    .orElse(null);

            if (outlineClient != null) {
                name = outlineClient.getTgLogin().equals("null") ? "друг" : "@" + outlineClient.getTgLogin();
            }

            String idSendTo = c;
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                log.error(
                        "Не удалось вставить паузу между отправками сообщений клиенту с tgId: '{}'!",
                        idSendTo
                );
            }
            SendPhoto photo = new SendPhoto(
                    idSendTo,
                    new InputFile(is, "Мигрируем с WireGuard на Outline")
            );
            photo.setCaption("""
                    Привет, %s :)
                                            
                    Последнее время приложение WireGuard работает плохо. Скорее всего вы это заметили и даже обратились в поддержку. Спасибо вам.
                                            
                    Мы решили проблему на корню — сменили приложение на более быстрое, безопасное и стабильное, а главное, которое не заблокируешь!
                                            
                    Отключаем, закрываем и удаляем <strike>WireGuard</strike> -> скачиваем, открываем и подключаем <b>Outline</b>!
                                            
                    Мы дарим вам 1 неделю бесплатного доступа за то, что вам придется сменить приложение. Всем! Даже тем, кто ни разу не пользовался. Стоит попробовать.
                     
                    Тем, кто уже получил ключ через поддержку — ничего делать не надо, для вас миграция бесшовна.
                                                
                    Чтобы подключиться, нажмите /instruction
                    """
                    .formatted(name));


            photo.setParseMode("HTML");
            log.info("Отправляем сообщение клиенту с tgId: '{}'", idSendTo);
            messages.add(photo);
        }
        return messages;
    }
}
