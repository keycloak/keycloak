package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.utils.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

public abstract class AbstractHttpGetRequest<R> {

    protected final AbstractOAuthClient<?> client;

    private HttpGet get;

    public AbstractHttpGetRequest(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    protected abstract String getEndpoint();

    protected abstract void initRequest();

    public R send() {
        get = new HttpGet(getEndpoint());
        get.addHeader("Accept", MediaType.APPLICATION_JSON);
        initRequest();
        try {
            return toResponse(client.httpClient().get().execute(get));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void header(String name, String value) {
        if (value != null) {
            get.addHeader(name, value);
        }
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

}
