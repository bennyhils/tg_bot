package bennyhils.inc.tgbot.model;

import bennyhils.inc.tgbot.util.DataTimeUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class OutlineClient {
    Integer id;
    String name;
    String tgLogin;
    String tgFirst;
    String tgLast;
    Instant createdAt;
    Instant updatedAt;
    Instant paidBefore;
    String password;
    Integer port;
    String method;
    DataLimit dataLimit;
    String accessUrl;

    @Getter
    @Setter
    public static class DataLimit {
        Long bytes;
    }


    public String toStringForFileWithDataUsage(String dataUsage) {
        accessUrl = dataLimit == null ? accessUrl : "Выключен";
        dataUsage = dataUsage != null ? dataUsage : "Не подключался";

        return
                "tgId: " + name +
                "\nЛогин: " + tgLogin +
                "\nИмя: " + tgFirst +
                "\nФамилия: " + tgLast +
                "\nОплачено до: " + DataTimeUtil.getNovosibirskTimeFromInstant(paidBefore) +
                "\nИспользовал трафика: " + dataUsage +
                "\nКлюч доступа: <code>" + accessUrl + "</code>";
    }
}
