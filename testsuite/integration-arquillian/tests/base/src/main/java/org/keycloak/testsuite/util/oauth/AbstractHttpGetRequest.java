package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.utils.MediaType;

import java.io.IOException;

public abstract class AbstractHttpGetRequest<T, R> {

    protected final OAuthClient client;

    public AbstractHttpGetRequest(OAuthClient client) {
        this.client = client;
    }

    protected abstract String getEndpoint();

    public R send() {
        HttpGet get = new HttpGet(getEndpoint());
        get.addHeader("Accept", MediaType.APPLICATION_JSON);
        try {
            return toResponse(client.httpClient().get().execute(get));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

}
