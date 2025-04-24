package bennyhils.inc.tgbot.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class LogHelper {

    public static void startLog(Instant startTime, String methodName) {
        log.debug("Метод '{}' начал свое выполнение в '{}'", methodName, startTime.toString());
    }

    public static void endLog(Instant startTime, Instant endTime, String methodName) {
        log.debug("Метод '{}' выполнился за: '{}' сек.", methodName, (endTime.toEpochMilli() - startTime.toEpochMilli()) / 1000);
    }
}
