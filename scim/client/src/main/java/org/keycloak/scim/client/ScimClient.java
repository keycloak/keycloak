package org.keycloak.scim.client;


import jakarta.ws.rs.core.Response.Status;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.scim.protocol.response.ErrorResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;

public final class ScimClient implements AutoCloseable {

    private static final String APPLICATION_SCIM_JSON = "application/scim+json";

    private final SimpleHttp http;
    private String baseUrl;

    private ScimClient(HttpClient http) {
        this.http = SimpleHttp.create(http);
    }

    public static Builder create(HttpClient httpClient) {
        return new Builder(httpClient);
    }

    public ScimUsersClient users() {
        return new ScimUsersClient(this);
    }

    public ScimGroupsClient groups() {
        return new ScimGroupsClient(this);
    }

    @Override
    public void close() {
        // no-op for now
    }

    SimpleHttpResponse execute(SimpleHttpRequest request) throws ScimClientException {
        try {
            SimpleHttpResponse response = request.asResponse();

            if (!Status.Family.familyOf(response.getStatus()).equals(Status.Family.SUCCESSFUL)) {
                ErrorResponse error = response.asJson(ErrorResponse.class);
                response.close();
                throw new ScimClientException("Error response from SCIM server", error);
            }

            return response;
        } catch (ScimClientException sce) {
            throw sce;
        } catch (Exception e) {
            throw new ScimClientException("Unexpected response from SCIM server", e);
        }
    }

    SimpleHttpRequest doGet(String path) {
        return beforeRequest(http.doGet(baseUrl + path))
                .header(HttpHeaders.ACCEPT, APPLICATION_SCIM_JSON);
    }

    SimpleHttpRequest doPost(String path) {
        return beforeRequest(http.doPost(baseUrl + path))
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_SCIM_JSON);
    }

    SimpleHttpRequest doDelete(String path) {
        return beforeRequest(http.doDelete(baseUrl + path));
    }

    SimpleHttpRequest doPut(String path) {
        return beforeRequest(http.doPut(baseUrl + path))
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_SCIM_JSON);
    }

    private SimpleHttpRequest beforeRequest(SimpleHttpRequest request) {
        return request;
    }

    private void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static class Builder {

        private static final String DEFAULT_API_PATH = "/scim/v2/";

        private final ScimClient client;

        private Builder(HttpClient baseUrl) {
            client = new ScimClient(baseUrl);
        }

        public Builder withBaseUrl(String baseUrl) {
            client.setBaseUrl(baseUrl + DEFAULT_API_PATH);
            return this;
        }

        public ScimClient build() {
            return client.connect();
        }
    }

    private ScimClient connect() {
        return this;
    }
}
