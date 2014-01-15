package org.keycloak.representations.adapters.config;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * Common Realm Configuration
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-not-required"})
public class BaseRealmConfig {
    @JsonProperty("realm")
    protected String realm;
    @JsonProperty("realm-public-key")
    protected String realmKey;
    @JsonProperty("auth-server-url")
    protected String authServerUrl;
    @JsonProperty("ssl-not-required")
    protected boolean sslNotRequired;

    public boolean isSslNotRequired() {
        return sslNotRequired;
    }

    public void setSslNotRequired(boolean sslNotRequired) {
        this.sslNotRequired = sslNotRequired;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealmKey() {
        return realmKey;
    }

    public void setRealmKey(String realmKey) {
        this.realmKey = realmKey;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }
}
