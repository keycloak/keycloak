package org.keycloak.client.registration;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistration {

    private final String DEFAULT = "default";
    private final String INSTALLATION = "install";

    private HttpUtil httpUtil;

    public ClientRegistration(String authServerUrl, String realm) {
        httpUtil = new HttpUtil(HttpClients.createDefault(), HttpUtil.getUrl(authServerUrl, "realms", realm, "clients"));
    }

    public ClientRegistration(String authServerUrl, String realm, HttpClient httpClient) {
        httpUtil = new HttpUtil(httpClient, HttpUtil.getUrl(authServerUrl, "realms", realm, "clients"));
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

    private String serialize(ClientRepresentation client) throws ClientRegistrationException {
        try {
            return JsonSerialization.writeValueAsString(client);
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to write json object", e);
        }
    }

    private <T> T deserialize(InputStream inputStream, Class<T> clazz) throws ClientRegistrationException {
        try {
            return JsonSerialization.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to read json object", e);
        }
    }

}
