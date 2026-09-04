package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Optional.ofNullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Shared JWE encryption config + the UserInfo test every variant runs. In the legacy suite every JWE
 * variant (the RSA base, the "just encrypted" UserInfo variant, and all eight ECDH-ES combinations)
 * extended {@code KcOidcBrokerJWETest}; this abstract class keeps that shared behavior, unlike the
 * legacy-era interim design (an interface trait), because every variant wants the full protocol config +
 * login tests + JWE stack together - there is no case here that needs to skip a layer, so a plain
 * extension of {@code AbstractKcOidcBrokerTest} expresses it without the getter boilerplate an interface
 * would force (its default methods can't call this class's protected fields/methods directly).
 */
public abstract class AbstractKcOidcJweBrokerTest extends AbstractKcOidcBrokerTest {

    protected String getEncAlg() {
        return JWEConstants.RSA_OAEP;
    }

    protected String getEncEnc() {
        return JWEConstants.A256GCM;
    }

    protected String getSigAlg() {
        return Algorithm.RS512;
    }

    protected ComponentRepresentation getConsumerKeyComponent() {
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

    protected ComponentRepresentation getProviderKeyComponent() {
        return null;
    }

    @BeforeEach
    void configureJweEncryption() {
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

    @Test
    public void testIdentityClaimsFromUserInfoEndpoint() {
        configureUserInfoEndpointMappers();
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        List<UserRepresentation> usersRep = getConsumerRealm().admin().users().search(getUserLogin(), true);
        assertFalse(usersRep.isEmpty());
        UserRepresentation userRep = usersRep.get(0);
        List<String> expectedAttribute = ofNullable(userRep.getAttributes())
                .orElse(Map.of()).getOrDefault("user-info", List.of());
        assertFalse(expectedAttribute.isEmpty());
        assertEquals("true", expectedAttribute.get(0));
    }

    private void configureUserInfoEndpointMappers() {
        ClientRepresentation client = getProviderRealm().admin().clients().findByClientId(CLIENT_ID).get(0);
        ClientResource clientResource = getProviderRealm().admin().clients().get(client.getId());

        ProtocolMapperRepresentation claimMapper = new ProtocolMapperRepresentation();
        claimMapper.setName("custom-claim-hardcoded-mapper");
        claimMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        claimMapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "user-info");
        config.put(HardcodedClaim.CLAIM_VALUE, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE, "false");
        claimMapper.setConfig(config);

        ProtocolMappersResource protocolMappers = clientResource.getProtocolMappers();
        List<ProtocolMapperRepresentation> mappers = protocolMappers
                .getMappersPerProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        ProtocolMapperRepresentation emailMapper = mappers.stream()
                .filter(m -> m.getConfig().getOrDefault(ProtocolMapperUtils.USER_ATTRIBUTE, "").equals("email"))
                .findAny().orElse(null);
        if (emailMapper != null) {
            protocolMappers.delete(emailMapper.getId());
        }
        protocolMappers.createMapper(claimMapper).close();

        IdentityProviderResource idp = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderMapperRepresentation attributeMapper = new IdentityProviderMapperRepresentation();
        attributeMapper.setName("attribute-mapper");
        attributeMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attributeMapper.setIdentityProviderAlias(getIdpAlias());
        attributeMapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString(),
                UserAttributeMapper.CLAIM, "user-info",
                UserAttributeMapper.USER_ATTRIBUTE, "user-info"));
        idp.addMapper(attributeMapper).close();
    }
}
