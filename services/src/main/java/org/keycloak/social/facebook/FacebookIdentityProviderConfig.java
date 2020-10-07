package org.keycloak.social.facebook;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.saml.common.util.StringUtil;

public class FacebookIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public FacebookIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public FacebookIdentityProviderConfig() {
    }

    public String getFetchedFields() {
        return Optional.ofNullable(getConfig().get("fetchedFields"))
                .map(fieldsConfig -> fieldsConfig.replaceAll("\\s+",""))
                .orElse("");
    }

    public void setFetchedFields(final String fetchedFields) {
        getConfig().put("fetchedFields", fetchedFields);
    }
}
