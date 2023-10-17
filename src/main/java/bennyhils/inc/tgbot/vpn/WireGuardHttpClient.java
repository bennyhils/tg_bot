package bennyhils.inc.tgbot.vpn;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class WireGuardHttpClient {

    public static String PORT = "51821";

    public HttpResponse<String> getSessionForCookie(String serverIp) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/session"))
                                         .setHeader("Content-Type", "application/json")
                                         .build();

        return sendRequest(request, serverIp);
    }

    public HttpResponse<String> getSession(String pass, String serverIp, String cookie) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/session"))
                                         .POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"" + pass + "\"}"))
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();

        return sendRequest(request, serverIp);
    }

    public HttpResponse<String> getClients(String serverIp, String session) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/wireguard/client"))
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", session)
                                         .build();

        return sendRequest(request, serverIp);
    }


    public HttpResponse<String> getPeerConfig(String id, String serverIp, String cookie) {

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/wireguard/client/" +
                                                         id +
                                                         "/configuration/"))
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();

        return sendRequest(request, serverIp);
    }

    public HttpResponse<String> getPeerQRCode(String id, String serverIp, String cookie) {

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/wireguard/client/" +
                                                         id +
                                                         "/qrcode.svg/"))
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();

        return sendRequest(request, serverIp);
    }

    public HttpResponse<String> createClient(User user, String serverIp, String cookie) {

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/wireguard/client"))
                                         .POST(HttpRequest.BodyPublishers
                                                 .ofString("{\"name\":\"" + user.getId() + "\"," +
                                                           "\"tgLogin\":\"" + user.getUserName() + "\"," +
                                                           "\"tgId\":\"" + user.getId() + "\"," +
                                                           "\"tgFirst\":\"" + user.getFirstName() + "\"," +
                                                           "\"tgLast\":\"" + user.getLastName() + "\"}"))
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();

        return sendRequest(request, serverIp);
    }

    public HttpResponse<String> updatePaidBefore(String clientId, String paidBefore, String serverIp, String cookie) {

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" +
                                                         serverIp +
                                                         ":" +
                                                         PORT +
                                                         "/api/wireguard/client/" +
                                                         clientId +
                                                         "/paid/"))
                                         .PUT(HttpRequest.BodyPublishers.ofString("{\"paidBefore\":\"" +
                                                                                  paidBefore +
                                                                                  "\"}"))
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();

        return sendRequest(request, serverIp);
    }


    public HttpResponse<String> disableClient(String clientId, String serverIp, String cookie) {

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/wireguard/client/" +
                                                         clientId + "/disable"))
                                         .POST(HttpRequest.BodyPublishers.noBody())
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();
        return sendRequest(request, serverIp);
    }

    public HttpResponse<String> enableClient(String clientId, String serverIp, String cookie) {

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + serverIp + ":" + PORT + "/api/wireguard/client/" +
                                                         clientId + "/enable"))
                                         .POST(HttpRequest.BodyPublishers.noBody())
                                         .setHeader("Content-Type", "application/json")
                                         .setHeader("Cookie", cookie)
                                         .build();

        return sendRequest(request, serverIp);
    }

    private HttpResponse<String> sendRequest(HttpRequest request, String serverIp) {
        try {
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("При обращении к серверу WireGuard '{}' возникла ошибка: '{}'", serverIp, e.getStackTrace());
            return null;
        }
    }
}
