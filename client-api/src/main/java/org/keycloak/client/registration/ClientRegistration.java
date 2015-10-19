package org.keycloak.client.registration;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.common.util.Base64;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistration {

    private String clientRegistrationUrl;
    private HttpClient httpClient;
    private Auth auth;

    public static ClientRegistrationBuilder create() {
        return new ClientRegistrationBuilder();
    }

    private ClientRegistration() {
    }

    public ClientRepresentation create(ClientRepresentation client) throws ClientRegistrationException {
        String content = serialize(client);
        InputStream resultStream = doPost(content);
        return deserialize(resultStream, ClientRepresentation.class);
    }

    public ClientRepresentation get() throws ClientRegistrationException {
        if (auth instanceof ClientIdSecretAuth) {
            String clientId = ((ClientIdSecretAuth) auth).clientId;
            return get(clientId);
        } else {
            throw new ClientRegistrationException("Requires client authentication");
        }
    }

    public ClientRepresentation get(String clientId) throws ClientRegistrationException {
        InputStream resultStream = doGet(clientId);
        return resultStream != null ? deserialize(resultStream, ClientRepresentation.class) : null;
    }

    public void update(ClientRepresentation client) throws ClientRegistrationException {
        String content = serialize(client);
        doPut(content, client.getClientId());
    }

    public void delete() throws ClientRegistrationException {
        if (auth instanceof ClientIdSecretAuth) {
            String clientId = ((ClientIdSecretAuth) auth).clientId;
            delete(clientId);
        } else {
            throw new ClientRegistrationException("Requires client authentication");
        }
    }

    public void delete(String clientId) throws ClientRegistrationException {
        doDelete(clientId);
    }

    public void close() throws ClientRegistrationException {
        if (httpClient instanceof CloseableHttpClient) {
            try {
                ((CloseableHttpClient) httpClient).close();
            } catch (IOException e) {
                throw new ClientRegistrationException("Failed to close http client", e);
            }
        }
    }

    private InputStream doPost(String content) throws ClientRegistrationException {
        try {
            HttpPost request = new HttpPost(clientRegistrationUrl);

            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setHeader(HttpHeaders.ACCEPT, "application/json");
            request.setEntity(new StringEntity(content));

            auth.addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 201) {
                return responseStream;
            } else {
                responseStream.close();
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    private InputStream doGet(String endpoint) throws ClientRegistrationException {
        try {
            HttpGet request = new HttpGet(clientRegistrationUrl + "/" + endpoint);

            request.setHeader(HttpHeaders.ACCEPT, "application/json");

            auth.addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 200) {
                return responseStream;
            } else if (response.getStatusLine().getStatusCode() == 404) {
                responseStream.close();
                return null;
            } else {
                responseStream.close();
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    private void doPut(String content, String endpoint) throws ClientRegistrationException {
        try {
            HttpPut request = new HttpPut(clientRegistrationUrl + "/" + endpoint);

            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setHeader(HttpHeaders.ACCEPT, "application/json");
            request.setEntity(new StringEntity(content));

            auth.addAuth(request);

            HttpResponse response = httpClient.execute(request);
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    private void doDelete(String endpoint) throws ClientRegistrationException {
        try {
            HttpDelete request = new HttpDelete(clientRegistrationUrl + "/" + endpoint);

            auth.addAuth(request);

            HttpResponse response = httpClient.execute(request);
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
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

    public static class ClientRegistrationBuilder {

        private String realm;

        private String authServerUrl;

        private Auth auth;

        private HttpClient httpClient;

        public ClientRegistrationBuilder realm(String realm) {
            this.realm = realm;
            return this;
        }
        public ClientRegistrationBuilder authServerUrl(String authServerUrl) {
            this.authServerUrl = authServerUrl;
            return this;
        }

        public ClientRegistrationBuilder auth(String token) {
            this.auth = new TokenAuth(token);
            return this;
        }

        public ClientRegistrationBuilder auth(String clientId, String clientSecret) {
            this.auth = new ClientIdSecretAuth(clientId, clientSecret);
            return this;
        }

        public ClientRegistrationBuilder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public ClientRegistration build() {
            ClientRegistration clientRegistration = new ClientRegistration();
            clientRegistration.clientRegistrationUrl = authServerUrl + "/realms/" + realm + "/client-registration/default";

            clientRegistration.httpClient = httpClient != null ? httpClient : HttpClients.createDefault();
            clientRegistration.auth = auth;

            return clientRegistration;
        }

    }

    public interface Auth {
        void addAuth(HttpRequest httpRequest);
    }

    public static class AuthorizationHeaderAuth implements Auth {
        private String credentials;

        public AuthorizationHeaderAuth(String credentials) {
            this.credentials = credentials;
        }

        public void addAuth(HttpRequest httpRequest) {
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, credentials);
        }
    }

    public static class TokenAuth extends AuthorizationHeaderAuth {
        public TokenAuth(String token) {
            super("Bearer " + token);
        }
    }

    public static class ClientIdSecretAuth extends AuthorizationHeaderAuth {
        private String clientId;

        public ClientIdSecretAuth(String clientId, String clientSecret) {
            super("Basic " + Base64.encodeBytes((clientId + ":" + clientSecret).getBytes()));
            this.clientId = clientId;
        }
    }

}
