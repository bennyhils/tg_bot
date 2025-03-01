package bennyhils.inc.tgbot.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Setter
@Getter
public class Server {

    private String cookie;
    private int clientsCount;
    private Set<String> clientsName;
    private List<Client> clients;

}
