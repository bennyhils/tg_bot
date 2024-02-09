package bennyhils.inc.tgbot.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DataTimeUtil {
    public final static DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm (новосибирское время)", Locale.forLanguageTag("ru-RU"));

    public final static DateTimeFormatter DATE_TIME_FORMATTER_FOR_FILE =
            DateTimeFormatter.ofPattern("yyyy.MM.d_HH:mm:ss.SSS", Locale.forLanguageTag("ru-RU"));

    public static String getNovosibirskTimeFromInstant(Instant instant) {
        if (instant == null) {
            instant = Instant.now();
        }
        ZonedDateTime zonedUTC = instant.atZone(ZoneId.of("UTC"));
        ZonedDateTime zonedIST = zonedUTC.withZoneSameInstant(ZoneId.of("Asia/Novosibirsk"));

        return zonedIST.format(DATE_TIME_FORMATTER);
    }

    public static String getFileNameForCheck(Instant instant) {
        if (instant == null) {
            instant = Instant.now();
        }
        ZonedDateTime zonedUTC = instant.atZone(ZoneId.of("UTC"));
        ZonedDateTime zonedIST = zonedUTC.withZoneSameInstant(ZoneId.of("Asia/Novosibirsk"));

        return zonedIST.format(DATE_TIME_FORMATTER_FOR_FILE);
    }
}
