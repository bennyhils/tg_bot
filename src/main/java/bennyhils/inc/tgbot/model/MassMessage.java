package bennyhils.inc.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MassMessage {
    private long id;
    private String message;
    private long picId;
    private long sentTo;
    private long deliveredTo;
}