package org.keycloak.broker.kubernetes;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderShowInAccountConsole;

public class KubernetesIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public KubernetesIdentityProviderConfig() {
        this(null);
    }

    public KubernetesIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
        setHideOnLogin(true);
        getConfig().put(IdentityProviderModel.SHOW_IN_ACCOUNT_CONSOLE, IdentityProviderShowInAccountConsole.NEVER.name());
    }

    @Override
    public boolean isHideOnLogin() {
        return true;
    }

}
