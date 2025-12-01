package org.keycloak.broker.kubernetes;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.Strings;

import static org.keycloak.common.util.UriUtils.checkUrl;

public class KubernetesIdentityProviderConfig extends IdentityProviderModel {

    public static final String ISSUER = OIDCIdentityProviderConfig.ISSUER;

    public KubernetesIdentityProviderConfig() {
    }

    public KubernetesIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    public int getAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    @Override
    public Boolean isHideOnLogin() {
        return true;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);

        String issuer = getIssuer();
        if (Strings.isEmpty(issuer)) {
            throw new IllegalArgumentException(ISSUER + " is required");
        }
        checkUrl(realm.getSslRequired(), issuer, ISSUER);
    }
}
