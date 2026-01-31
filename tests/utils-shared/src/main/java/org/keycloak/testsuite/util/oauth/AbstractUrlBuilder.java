package org.keycloak.testsuite.util.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.util.JsonSerialization;

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

    protected void parameter(String name, Object value) {
        String encoded = URLEncoder.encode(JsonSerialization.valueAsString(value), StandardCharsets.UTF_8);
        parameter(name, encoded);
    }

    public String build() {
        UriBuilder uriBuilder = UriBuilder.fromUri(getEndpoint());
        params.entrySet().stream().filter(e -> e.getValue() != null).forEach(e -> uriBuilder.queryParam(e.getKey(), e.getValue()));
        return uriBuilder.build().toString();
    }

}
