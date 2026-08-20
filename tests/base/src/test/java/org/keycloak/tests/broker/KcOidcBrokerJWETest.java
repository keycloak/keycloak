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
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Test;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest
public class KcOidcBrokerJWETest implements JweBrokerConfigSupport, BrokerLoginTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @Test
    public void testIdentityClaimsFromUserInfoEndpoint() {
        configureUserInfoEndpointMappers();
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        List<UserRepresentation> usersRep = consumerRealm.admin().users().search(getUserLogin(), true);
        assertFalse(usersRep.isEmpty());
        UserRepresentation userRep = usersRep.get(0);
        List<String> expectedAttribute = ofNullable(userRep.getAttributes())
                .orElse(Map.of()).getOrDefault("user-info", List.of());
        assertFalse(expectedAttribute.isEmpty());
        assertEquals("true", expectedAttribute.get(0));
    }

    private void configureUserInfoEndpointMappers() {
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0);
        ClientResource clientResource = providerRealm.admin().clients().get(client.getId());

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

        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
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
