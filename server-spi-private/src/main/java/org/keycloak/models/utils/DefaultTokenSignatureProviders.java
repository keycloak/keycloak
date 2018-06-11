package org.keycloak.models.utils;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.jose.jws.TokenSignatureProvider;
import org.keycloak.models.RealmModel;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class DefaultTokenSignatureProviders {
    private static final String COMPONENT_SIGNATURE_ALGORITHM_KEY = "org.keycloak.jose.jws.TokenSignatureProvider.algorithm";
    private static final String RSASSA_PROVIDER_ID = "rsassa-signature";
    private static final String HMAC_PROVIDER_ID = "hmac-signature";

    public static void createProviders(RealmModel realm) {
       createAndAddProvider(realm, RSASSA_PROVIDER_ID, "RS256");
       createAndAddProvider(realm, RSASSA_PROVIDER_ID, "RS384");
       createAndAddProvider(realm, RSASSA_PROVIDER_ID, "RS512");
       createAndAddProvider(realm, HMAC_PROVIDER_ID, "HS256");
       createAndAddProvider(realm, HMAC_PROVIDER_ID, "HS384");
       createAndAddProvider(realm, HMAC_PROVIDER_ID, "HS512");
    }

    private static void createAndAddProvider(RealmModel realm, String providerId, String sigAlgName) {
        ComponentModel generated = new ComponentModel();
        generated.setName(providerId);
        generated.setParentId(realm.getId());
        generated.setProviderId(providerId);
        generated.setProviderType(TokenSignatureProvider.class.getName());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(COMPONENT_SIGNATURE_ALGORITHM_KEY, sigAlgName);
        generated.setConfig(config);
        realm.addComponentModel(generated);
    }
}
