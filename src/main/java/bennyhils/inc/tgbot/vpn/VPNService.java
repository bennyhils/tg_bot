package bennyhils.inc.tgbot.vpn;

import bennyhils.inc.tgbot.model.OutlineClient;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

public interface VPNService {

    List<OutlineClient> getAllServersClients(Properties properties);

    OutlineClient createClient(String server, int freeDaysPeriod, String tgId, String tgLogin, String tgFirst, String tgLast);

    void updatePaidBefore(String server, Instant paidBefore, String tgId);

    void deleteClient(String server, String id);

    void disableClient(String server, String id);

    void enableClient(String server, String id);

}
