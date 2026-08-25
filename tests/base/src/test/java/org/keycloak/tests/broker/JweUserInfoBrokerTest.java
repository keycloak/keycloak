package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.jupiter.api.Test;

import static java.util.Optional.ofNullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Shared JWE UserInfo test. In the legacy suite every JWE variant (the RSA base, the "just encrypted"
 * UserInfo variant, and all eight ECDH-ES combinations) extended {@code KcOidcBrokerJWETest} and thus
 * ran {@code testIdentityClaimsFromUserInfoEndpoint()}. Sharing it as a default method here keeps that
 * per-variant coverage without re-introducing the abstract test-class hierarchy.
 */
public interface JweUserInfoBrokerTest extends JweBrokerConfigSupport {

    @Test
    default void testIdentityClaimsFromUserInfoEndpoint() {
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

    default void configureUserInfoEndpointMappers() {
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
