package bennyhils.inc.tgbot.util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DataTimeUtil {
    public final static DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm (Всемирное Время), d MMMM yyyy", Locale.forLanguageTag("ru-RU"));
}
