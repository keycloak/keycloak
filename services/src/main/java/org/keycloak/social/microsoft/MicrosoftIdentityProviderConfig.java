package org.keycloak.social.microsoft;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class MicrosoftIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public MicrosoftIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public MicrosoftIdentityProviderConfig() {
        
    }

    public String getTenantId() {
        String tenantId = getConfig().get("tenantId");

        return tenantId == null || tenantId.isEmpty() ? null : tenantId;
    }

    public void setTenantId(final String tenantId) {
        getConfig().put("tenantId", tenantId);
    }
}
