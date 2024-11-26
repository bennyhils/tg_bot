package bennyhils.inc.tgbot.vpn;

import bennyhils.inc.tgbot.model.OutlineClient;
import bennyhils.inc.tgbot.model.ServerOutlineNative;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.research.ws.wadl.HTTPMethods;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OutlineHttpClient {

    private final static int TIMEOUT_MS = 30_000;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private final static MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.parse("application/json");
    private final static RequestBody EMPTY_BODY = RequestBody.create(MediaType.parse("text/plain"), "");

    public OutlineClient createClient(String server) {
        try {
            Response execute;
            execute = execute(HTTPMethods.POST, EMPTY_BODY, server + "/access-keys");
            ResponseBody body = execute.body();
            String resp = null;
            if (body != null) {
                resp = body.string();
            }
            execute.close();

            return OBJECT_MAPPER.readValue(resp, OutlineClient.class);

        } catch (IOException e) {
            log.error("Не удалось создать клиента на сервере: '{}'. Ошибка: '{}'", server, e.getMessage());

            return null;
        }
    }

    public void renameClient(String server, String id, String tgId) {
        RequestBody body = RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, """
                {"name": "%s"}
                """.formatted(tgId));
        String url = server + "/access-keys/" + id + "/name";
        Response execute;
        execute = execute(HTTPMethods.PUT, body, url);
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалось переименовать на сервере: '{}' клиента с id: '{}'", server, id);
        }

    }

    public void updateClientTgData(String server, String id, String tgLogin, String tgFirst, String tgLast) {
        RequestBody body = RequestBody.create(
                MEDIA_TYPE_APPLICATION_JSON, """
                        {"tgLogin": "%s", "tgFirst": "%s", "tgLast": "%s"}
                        """.formatted(tgLogin, tgFirst, tgLast)
        );
        String url = server + "/access-keys/" + id + "/tgData";
        Response execute;
        execute = execute(HTTPMethods.PUT, body, url);
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалось обновить данные Телеграм на сервере: '{}' у клиента с id: '{}'", server, id);
        }
    }

    public void updatePaidBefore(String server, String id, Instant paidBefore) {
        RequestBody body = RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, """
                {"paidBefore": "%s"}
                """.formatted(paidBefore.toString())
        );
        String url = server + "/access-keys/" + id + "/paidBefore";
        Response execute;
        execute = execute(HTTPMethods.PUT, body, url);
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалось обновить дату подписки на сервере: '{}' у клиента с id: '{}'", server, id);
        }
    }

    public OutlineClient getClient(String server, String id) {
        String url = server + "/access-keys/" + id;

        String client = getStringClients(url);
        try {
            Response execute;
            execute = execute(HTTPMethods.GET, null, url);
            ResponseBody body = execute.body();
            if (body != null) {
                client = body.string();
            }
            execute.close();

            return OBJECT_MAPPER.readValue(client, OutlineClient.class);
        } catch (IOException e) {
            log.error("С сервера: '{}' не удалось получить клиента с id: '{}'", server, id);
            return null;
        }
    }

    public List<OutlineClient> getClients(String server) {
        String url = server + "/access-keys";

        String clients = getStringClients(url);
        JSONObject jsnObject;
        JSONArray jsonArray = new JSONArray();
        if (clients != null) {
            jsnObject = new JSONObject(clients);
            jsonArray = jsnObject.getJSONArray("accessKeys");
        }

        List<OutlineClient> outlineClients = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            OutlineClient outlineClient = null;
            try {
                outlineClient = OBJECT_MAPPER.readValue(jsonArray.get(i).toString(), OutlineClient.class);
            } catch (JsonProcessingException e) {
                log.error("Не удалось получить клиента: {}", e.getMessage());
            }
            outlineClients.add(outlineClient);
        }

        return outlineClients;
    }

    public Map<String, Long> getDataUsage(String server) {
        String url = server + "/metrics/transfer";
        Response execute;
        String response;
        try {
            execute = execute(HTTPMethods.GET, null, url);
            ResponseBody body = execute.body();

            if (body != null) {
                response = body.string();
                execute.close();
            } else {
                log.error("Не удалось получить трафик клиентов на сервере: '{}'", server);

                return null;
            }
        } catch (IOException e) {
            log.error("Не удалось получить трафик клиентов на сервере: '{}'", server);

            return null;
        }
        JSONObject jsonObject = (JSONObject) new JSONObject(response).get("bytesTransferredByUserId");
        Map<String, Object> stringObjectMap = jsonObject.toMap();
        Map<String, Long> bytesTransferredByUserId = new HashMap<>();
        for (String k : stringObjectMap.keySet()) {
            bytesTransferredByUserId.put(k, Long.valueOf(stringObjectMap.get(k).toString()));
        }

        return bytesTransferredByUserId;
    }


    public void deleteClient(String server, String id) {
        Response execute;
        execute = execute(HTTPMethods.DELETE, EMPTY_BODY, server + "/access-keys/" + id);
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалить на сервере: '{}' клиента с id: '{}'", server, id);
        }
    }

    public void setAccessKeyDataLimit(String server, String id) {

        RequestBody body = RequestBody.create(
                MEDIA_TYPE_APPLICATION_JSON,
                """
                        {"limit":{"bytes": 0}}"""
        );

        Response execute;
        execute = execute(HTTPMethods.PUT, body, server + "/access-keys/" + id + "/data-limit");
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалось установить лимит трафика на сервере: '{}' для клиента с id: '{}'", server, id);
        }
    }

    public void removeAccessKeyDataLimit(String server, String id) {
        Response execute;
        execute = execute(HTTPMethods.DELETE, EMPTY_BODY, server + "/access-keys/" + id + "/data-limit");
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалось удалить лимит трафика на сервере: '{}' для клиента с id: '{}'", server, id);
        }
    }

    public void updateCreatedAtAndUpdatedAt(String server, String id, Instant createdAt, Instant updatedAt) {
        RequestBody body =
                RequestBody.create(
                        MEDIA_TYPE_APPLICATION_JSON,
                        """
                                {"createdAt": "%s", "updatedAt": "%s"}""".formatted(
                                createdAt.toString(),
                                updatedAt.toString()
                        )
                );

        Response execute;
        execute = execute(HTTPMethods.PUT, body, server + "/access-keys/" + id + "/createdAtAndUpdatedAt");
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error(
                    "Не удалось обновить время создания и последнего изменения на сервере: '{}' для клиента с id: '{}'",
                    server,
                    id
            );
        }
    }

    public ServerOutlineNative getServerNative(String server) {
        String url = server + "/server";

        ServerOutlineNative serverOutlineNative;

        Response execute;
        execute = execute(HTTPMethods.GET, null, url);
        ResponseBody body = execute.body();
        String serverNativeString = null;
        if (body != null) {
            try {
                serverNativeString = body.string();
            } catch (IOException e) {
                log.error("Не удалось получить информацию о сервере с ошибкой: '{}'", e.getMessage());
            }
        }
        execute.close();

        try {
            serverOutlineNative = OBJECT_MAPPER.readValue(serverNativeString, ServerOutlineNative.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось получить информацию о сервере: {}", e.getMessage());

            return null;
        }

        return serverOutlineNative;
    }

    public void setPortForNewAccessKeys(String server, int port) {

        RequestBody body = RequestBody.create(
                MEDIA_TYPE_APPLICATION_JSON,
                """
                        {"port": %s}""".formatted(port)
        );

        Response execute;
        execute = execute(HTTPMethods.PUT, body, server + "/server/port-for-new-access-keys");
        int code = execute.code();
        execute.close();
        if (code != 204) {
            log.error("Не удалось установить на сервере: '{}' новый порт: '{}'", server, port);
        }
    }

    @Nullable
    private String getStringClients(String url) {
        Response execute;
        execute = execute(HTTPMethods.GET, null, url);
        ResponseBody body = execute.body();
        String clients = null;
        if (body != null) {
            try {
                clients = body.string();
            } catch (IOException e) {
                log.error("Не удалось получить клиента(ов) с ошибкой: '{}'", e.getMessage());
            }
        }
        execute.close();
        return clients;
    }

    private Response execute(HTTPMethods method, RequestBody body, String url) {
        Request request = new Request.Builder()
                .url(url)
                .method(method.value(), body)
                .addHeader("Content-Type", "application/json")
                .build();

        try {
            Response execute;
            execute = createClient()
                    .newCall(request)
                    .execute();
            return execute;

        } catch (IOException e) {
            log.error("Неудачный вызов метода: '{}' с ошибкой: '{}'", url, e.getMessage());

            throw new RuntimeException(e.getMessage());
        }
    }


    private TrustManager[] trustManager() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

    private SSLContext sslContext() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, trustManager(), new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return sslContext;
    }

    private OkHttpClient createClient() {
        OkHttpClient.Builder newBuilder = new OkHttpClient.Builder();
        newBuilder.sslSocketFactory(sslContext().getSocketFactory(), (X509TrustManager) trustManager()[0]);
        newBuilder.hostnameVerifier((hostname, session) -> true);
        newBuilder.setCallTimeout$okhttp(TIMEOUT_MS);
        newBuilder.setConnectTimeout$okhttp(TIMEOUT_MS);
        newBuilder.setReadTimeout$okhttp(TIMEOUT_MS);
        newBuilder.setWriteTimeout$okhttp(TIMEOUT_MS);

        return newBuilder.build();
    }
}
