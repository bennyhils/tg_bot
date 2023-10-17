package bennyhils.inc.tgbot.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DataTimeUtil {
    public final static DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm (новосибирское время), d MMMM yyyy", Locale.forLanguageTag("ru-RU"));

    public static String getNovosibirskTimeFromInstant(Instant instant) {
        if (instant == null) {
            return "Время не указано";
        }
        ZonedDateTime zonedUTC = instant.atZone(ZoneId.of("UTC"));
        ZonedDateTime zonedIST = zonedUTC.withZoneSameInstant(ZoneId.of("Asia/Novosibirsk"));

        return zonedIST.format(DATE_TIME_FORMATTER);
    }
}
