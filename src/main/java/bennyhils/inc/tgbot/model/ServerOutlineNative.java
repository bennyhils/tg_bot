package bennyhils.inc.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ServerOutlineNative {

    private String name;
    private String serverId;
    private boolean metricsEnabled;
    private long createdTimestampMs;
    private String version;
    private int portForNewAccessKeys;
    private String hostnameForAccessKeys;
}
