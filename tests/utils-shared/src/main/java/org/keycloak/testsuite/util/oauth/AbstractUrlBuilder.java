package org.keycloak.testsuite.util.oauth;

import jakarta.ws.rs.core.UriBuilder;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractUrlBuilder {

    protected final AbstractOAuthClient<?> client;
    protected Map<String, String> params = new HashMap<>();

    public AbstractUrlBuilder(AbstractOAuthClient<?> client) {
        this.client = client;
        initRequest();
    }

    public abstract String getEndpoint();

    protected abstract void initRequest();

    public void open() {
        client.driver.navigate().to(build());
    }

    protected void parameter(String name, String value) {
        params.put(name, value);
    }

    protected void replaceParameter(String name, String value) {
        params.put(name, value);
    }

    public String build() {
        UriBuilder uriBuilder = UriBuilder.fromUri(getEndpoint());
        params.entrySet().stream().filter(e -> e.getValue() != null).forEach(e -> uriBuilder.queryParam(e.getKey(), e.getValue()));
        return uriBuilder.build().toString();
    }

}
