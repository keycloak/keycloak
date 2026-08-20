package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;

import org.junit.jupiter.api.BeforeEach;

public interface JweBrokerConfigSupport extends OidcBrokerConfigSupport {

    default String getEncAlg() {
        return JWEConstants.RSA_OAEP;
    }

    default String getEncEnc() {
        return JWEConstants.A256GCM;
    }

    default String getSigAlg() {
        return Algorithm.RS512;
    }

    default ComponentRepresentation getConsumerKeyComponent() {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setName("rsa-enc-generated");
        component.setProviderId("rsa-enc-generated");
        component.setProviderType(KeyProvider.class.getName());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        config.putSingle("keyUse", KeyUse.ENC.name());
        config.putSingle("algorithm", getEncAlg());
        component.setConfig(config);
        return component;
    }

    default ComponentRepresentation getProviderKeyComponent() {
        return null;
    }

    @BeforeEach
    default void configureJweEncryption() {
        ComponentRepresentation consumerKey = getConsumerKeyComponent();
        if (consumerKey != null) {
            getConsumerRealm().admin().components().add(consumerKey).close();
        }

        ComponentRepresentation providerKey = getProviderKeyComponent();
        if (providerKey != null) {
            getProviderRealm().admin().components().add(providerKey).close();
        }

        String consumerBaseUrl = getConsumerRealm().getBaseUrl();
        List<ClientRepresentation> clients = getProviderRealm().admin().clients().findByClientId(CLIENT_ID);
        ClientRepresentation client = clients.get(0);
        Map<String, String> attrs = client.getAttributes();

        attrs.put(OIDCConfigAttributes.USE_JWKS_URL, "true");
        attrs.put(OIDCConfigAttributes.JWKS_URL,
                consumerBaseUrl + "/protocol/openid-connect/certs");

        String encAlg = getEncAlg();
        if (encAlg != null) {
            attrs.put(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG, encAlg);
            attrs.put(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ALG, encAlg);
        }

        String encEnc = getEncEnc();
        if (encEnc != null) {
            attrs.put(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC, encEnc);
            attrs.put(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ENC, encEnc);
        }

        String sigAlg = getSigAlg();
        if (sigAlg != null) {
            attrs.put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, sigAlg);
            attrs.put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, sigAlg);
        }

        getProviderRealm().admin().clients().get(client.getId()).update(client);
    }
}
