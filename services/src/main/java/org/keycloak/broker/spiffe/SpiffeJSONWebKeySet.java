package org.keycloak.broker.spiffe;

import org.keycloak.jose.jwk.JSONWebKeySet;

import com.fasterxml.jackson.annotation.JsonProperty;

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
