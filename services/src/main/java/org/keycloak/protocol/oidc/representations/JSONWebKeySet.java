package org.keycloak.protocol.oidc.representations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.jose.jwk.JWK;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JSONWebKeySet {

    @JsonProperty("keys")
    private JWK[] keys;

    public JWK[] getKeys() {
        return keys;
    }

    public void setKeys(JWK[] keys) {
        this.keys = keys;
    }

}
