package org.keycloak.broker.spiffe;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.jose.jwk.JSONWebKeySet;

public class SpiffeJSONWebKeySet extends JSONWebKeySet {

    @JsonProperty("spiffe_refresh_hint")
    private Long spiffeRefreshHint;

    public Long getSpiffeRefreshHint() {
        return spiffeRefreshHint;
    }

    public void setSpiffeRefreshHint(Long spiffeRefreshHint) {
        this.spiffeRefreshHint = spiffeRefreshHint;
    }
}
