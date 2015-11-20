package org.keycloak.client.registration;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.ObjectMapper;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistration {

    public static final ObjectMapper outputMapper = new ObjectMapper();
    static {
        outputMapper.getSerializationConfig().addMixInAnnotations(ClientRepresentation.class, ClientRepresentationMixIn.class);
    }

    private final String DEFAULT = "default";
    private final String INSTALLATION = "install";

    private HttpUtil httpUtil;

    public static ClientRegistrationBuilder create() {
        return new ClientRegistrationBuilder();
    }

    ClientRegistration(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public void close() throws ClientRegistrationException {
        if (httpUtil != null) {
            httpUtil.close();
        }
        httpUtil = null;
    }

    public ClientRegistration auth(Auth auth) {
        httpUtil.setAuth(auth);
        return this;
    }

    public ClientRepresentation create(ClientRepresentation client) throws ClientRegistrationException {
        String content = serialize(client);
        InputStream resultStream = httpUtil.doPost(content, DEFAULT);
        return deserialize(resultStream, ClientRepresentation.class);
    }

    public ClientRepresentation get(String clientId) throws ClientRegistrationException {
        InputStream resultStream = httpUtil.doGet(DEFAULT, clientId);
        return resultStream != null ? deserialize(resultStream, ClientRepresentation.class) : null;
    }

    public AdapterConfig getAdapterConfig(String clientId) throws ClientRegistrationException {
        InputStream resultStream = httpUtil.doGet(INSTALLATION, clientId);
        return resultStream != null ? deserialize(resultStream, AdapterConfig.class) : null;
    }

    public ClientRepresentation update(ClientRepresentation client) throws ClientRegistrationException {
        String content = serialize(client);
        InputStream resultStream = httpUtil.doPut(content, DEFAULT, client.getClientId());
        return resultStream != null ? deserialize(resultStream, ClientRepresentation.class) : null;
    }

    public void delete(ClientRepresentation client) throws ClientRegistrationException {
        delete(client.getClientId());
    }

    public void delete(String clientId) throws ClientRegistrationException {
        httpUtil.doDelete(DEFAULT, clientId);
    }

    public static String serialize(ClientRepresentation client) throws ClientRegistrationException {
        try {

            return outputMapper.writeValueAsString(client);
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to write json object", e);
        }
    }

    private static <T> T deserialize(InputStream inputStream, Class<T> clazz) throws ClientRegistrationException {
        try {
            return JsonSerialization.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to read json object", e);
        }
    }

    public static class ClientRegistrationBuilder {

        private String url;
        private HttpClient httpClient;

        ClientRegistrationBuilder() {
        }

        public ClientRegistrationBuilder url(String realmUrl) {
            url = realmUrl;
            return this;
        }

        public ClientRegistrationBuilder url(String authUrl, String realm) {
            url = HttpUtil.getUrl(authUrl, "realms", realm, "clients");
            return this;
        }

        public ClientRegistrationBuilder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public ClientRegistration build() {
            if (url == null) {
                throw new IllegalStateException("url not configured");
            }

            if (httpClient == null) {
                httpClient = HttpClients.createDefault();
            }

            return new ClientRegistration(new HttpUtil(httpClient, url));
        }

    }

}
