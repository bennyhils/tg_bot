package bennyhils.inc.tgbot.model;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Setter
@Getter
public class WireGuardClient implements Comparable<WireGuardClient> {
    private String name;
    private String id;
    private String tgLogin;
    private String tgId;
    private String tgFirst;
    private String tgLast;
    private String address;
    private String privateKey;
    private String publicKey;
    private String preSharedKey;
    private String latestHandshakeAt;
    private String persistentKeepalive;
    private long transferTx;
    private long transferRx;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidBefore;
    boolean enabled;

    @Override
    public int compareTo(@NotNull WireGuardClient wireGuardClient) {
        return this.createdAt.compareTo(wireGuardClient.createdAt);
    }
}
