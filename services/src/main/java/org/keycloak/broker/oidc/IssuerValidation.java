package org.keycloak.broker.oidc;

import java.util.Map;
import java.util.Objects;

import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.util.Strings;
import org.keycloak.utils.KeycloakSessionUtil;

import static org.keycloak.common.util.UriUtils.checkUrl;
import static org.keycloak.models.IdentityProviderModel.ISSUER;

public interface IssuerValidation {

    Map<String, String> getConfig();

    String getInternalId();

    boolean isEnabled();

    default void validateIssuer(RealmModel realm) {

        String issuer = getConfig().get(ISSUER);
        if (Strings.isEmpty(issuer)) {
            throw new IllegalArgumentException("Issuer is required");
        }

        checkUrl(realm.getSslRequired(), issuer, "Issuer");

        if (isEnabled()) {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            AlternativeLookupProvider lookupProvider = session.getProvider(AlternativeLookupProvider.class);

            if (lookupProvider != null) {
                IdentityProviderModel existingIdp = lookupProvider.lookupIdentityProviderFromIssuer(session, getConfig().get(ISSUER));
                if (existingIdp != null && (getInternalId() == null || !Objects.equals(existingIdp.getInternalId(), getInternalId()))) {
                    throw new IllegalArgumentException("Issuer URL already used for IDP '" + existingIdp.getAlias() + "', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication");
                }
            }
        }
    }
}
