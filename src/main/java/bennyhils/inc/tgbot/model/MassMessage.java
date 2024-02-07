package bennyhils.inc.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MassMessage implements Comparable<MassMessage> {
    private long id;
    private String message;
    private long picId;
    private Instant sendingTime;
    private long sentTo;
    private long deliveredTo;

    @Override
    public int compareTo(@NotNull MassMessage massMessage) {
        return Long.compare(this.id, massMessage.id);
    }
}