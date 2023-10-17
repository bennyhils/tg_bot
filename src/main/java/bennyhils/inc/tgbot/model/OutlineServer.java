package bennyhils.inc.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
public class OutlineServer {

    private int clientsCount;
    private Set<String> tgIds;
    private List<OutlineClient> clients;
}
