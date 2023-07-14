package bennyhils.inc.tgbot.wireguard;

import bennyhils.inc.tgbot.model.Client;
import bennyhils.inc.tgbot.model.Server;
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

    public Map<String, Client> getClientByTgId(Properties properties, String tgId) {

        String clientServer = null;
        Client client = null;

        Map<String, Server> serversClients = getServersClients(properties);
        for (String server : serversClients.keySet()) {
            if (serversClients.get(server).getClientsName().contains(tgId)) {
                clientServer = server;
                break;
            }
        }

        if (clientServer == null) {
            return null;
        }

        Server server = serversClients.get(clientServer);
        if (server == null) {
            return null;
        }

        List<Client> clients = server.getClients();
        for (Client currentClient : clients) {
            if (currentClient.getTgId().equals(tgId)) {
                client = currentClient;
                break;
            }
        }

        Map<String, Client> serverClient = new HashMap<>();

        if (client == null) {
            return null;
        }

        serverClient.put(clientServer, client);

        return serverClient;
    }

    public Map<String, Server> getServersClients(Properties config) {
        List<Map<String, String>> serversMap = new ArrayList<>();
        String servers = config.getProperty("servers");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        try {
            serversMap = objectMapper.readValue(servers, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить в JSON: {}", e.getMessage());
        }
        Map<String, Server> serversClients = new HashMap<>();
        for (Map<String, String> server : serversMap) {
            String serverIp = server.get("ip");
            WireGuardClient wireGuardClient = new WireGuardClient();
            HttpResponse<String> session = wireGuardClient.getSessionForCookie(serverIp);
            HttpResponse<String> admin =
                    wireGuardClient.getSession(server.get("passWGUI"), serverIp, session.headers().map().get("set-cookie").get(0));
            HttpHeaders headers = admin.request().headers();
            String cookie = headers.map().get("cookie").get(0);

            try {
                List<Client> allClients = objectMapper.readValue(
                        wireGuardClient.getClients(serverIp, cookie).body(), new TypeReference<>() {
                        });
                Set<String> clientsName = new HashSet<>();
                for (Client client : allClients) {
                    clientsName.add(client.getName());
                }
                Server server1 = new Server();
                server1.setCookie(cookie);
                server1.setClientsCount(clientsName.size());
                server1.setClientsName(clientsName);
                server1.setClients(allClients);
                serversClients.put(serverIp, server1);
            } catch (JsonProcessingException e) {
                log.error("Не удалось распарсить в JSON: {}", e.getMessage());
            }
        }
        return serversClients;
    }
}
