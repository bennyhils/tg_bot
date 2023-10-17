package bennyhils.inc.tgbot.vpn;

import bennyhils.inc.tgbot.model.WireGuardClient;
import bennyhils.inc.tgbot.model.WireGuardServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class WireGuardService {

    public Map<String, WireGuardClient> getClientByTgId(Properties properties, String tgId) {

        String clientServer = null;
        WireGuardClient wireGuardClient = null;

        Map<String, WireGuardServer> serversClients = getServersClients(properties);
        for (String server : serversClients.keySet()) {
            if (serversClients.get(server).getClientsName().contains(tgId)) {
                clientServer = server;
                break;
            }
        }

        if (clientServer == null) {
            return null;
        }

        WireGuardServer wireGuardServer = serversClients.get(clientServer);
        if (wireGuardServer == null) {
            return null;
        }

        List<WireGuardClient> wireGuardClients = wireGuardServer.getWireGuardClients();
        for (WireGuardClient currentWireGuardClient : wireGuardClients) {
            if (currentWireGuardClient.getTgId().equals(tgId)) {
                wireGuardClient = currentWireGuardClient;
                break;
            }
        }

        Map<String, WireGuardClient> serverClient = new HashMap<>();

        if (wireGuardClient == null) {
            return null;
        }

        serverClient.put(clientServer, wireGuardClient);

        return serverClient;
    }

    public Map<String, WireGuardServer> getServersClients(Properties config) {
        List<Map<String, String>> serversMap = new ArrayList<>();
        String servers = config.getProperty("servers.wireguard");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        try {
            serversMap = objectMapper.readValue(servers, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить в JSON: {}", e.getMessage());
        }
        Map<String, WireGuardServer> serversClients = new HashMap<>();
        for (Map<String, String> server : serversMap) {
            String serverIp = server.get("ip");
            WireGuardHttpClient wireGuardHttpClient = new WireGuardHttpClient();
            HttpResponse<String> session = wireGuardHttpClient.getSessionForCookie(serverIp);
            HttpResponse<String> admin =
                    wireGuardHttpClient.getSession(
                            server.get("passWGUI"),
                            serverIp,
                            session.headers().map().get("set-cookie").get(0)
                    );
            HttpHeaders headers = admin.request().headers();
            String cookie = headers.map().get("cookie").get(0);

            try {
                List<WireGuardClient> allWireGuardClients = objectMapper.readValue(
                        wireGuardHttpClient.getClients(serverIp, cookie).body(), new TypeReference<>() {
                        });
                Set<String> clientsName = new HashSet<>();
                for (WireGuardClient client : allWireGuardClients) {
                    clientsName.add(client.getName());
                }
                WireGuardServer wireGuardServer1 = new WireGuardServer();
                wireGuardServer1.setCookie(cookie);
                wireGuardServer1.setClientsCount(clientsName.size());
                wireGuardServer1.setClientsName(clientsName);
                wireGuardServer1.setWireGuardClients(allWireGuardClients);
                serversClients.put(serverIp, wireGuardServer1);
            } catch (JsonProcessingException e) {
                log.error("Не удалось распарсить в JSON: {}", e.getMessage());
            }
        }
        return serversClients;
    }
}
