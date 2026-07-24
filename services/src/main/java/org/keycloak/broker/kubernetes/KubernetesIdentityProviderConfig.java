package org.keycloak.broker.kubernetes;


import org.keycloak.broker.oidc.IssuerValidation;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.RealmModel;
import org.keycloak.util.Strings;

import static org.keycloak.broker.kubernetes.KubernetesConstants.DEFAULT_KUBERNETES_ISSUER_URL;


public class KubernetesIdentityProviderConfig extends IdentityProviderModel implements IssuerValidation {

    public KubernetesIdentityProviderConfig() {
    }

    public KubernetesIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        String issuer = getConfig().get(ISSUER);
        if (Strings.isEmpty(issuer)) {
            return DEFAULT_KUBERNETES_ISSUER_URL;
        }

        return issuer;
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
        getConfig().put(ISSUER, getIssuer());
        validateIssuer(realm, IdentityProviderType.CLIENT_ASSERTION);
    }
}
