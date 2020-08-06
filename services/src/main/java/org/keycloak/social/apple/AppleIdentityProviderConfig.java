package org.keycloak.social.apple;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

/**
 * @author Emilien Bondu
 */
public class AppleIdentityProviderConfig extends OIDCIdentityProviderConfig {

    private static final String TEAM_ID = "teamId";

    private static final String KEY_ID = "keyId";

    private static final String P8_CONTENT = "p8Content";

    public AppleIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public AppleIdentityProviderConfig() {
    }

    public String getTeamId() {
        return getConfig().get(TEAM_ID);
    }

    public void setTeamId(String teamId) {
        getConfig().put(TEAM_ID, teamId);
    }

    public String getKeyId() {
        return getConfig().get(KEY_ID);
    }

    public void setKeyId(String keyId) {
        getConfig().put(KEY_ID, keyId);
    }

    public String getP8Content() {
        return getConfig().get(P8_CONTENT);
    }

    public void setP8Content(String p8Content) {
        getConfig().put(P8_CONTENT, p8Content);
    }
}
