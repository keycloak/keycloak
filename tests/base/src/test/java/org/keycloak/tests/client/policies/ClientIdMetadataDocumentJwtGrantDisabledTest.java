package org.keycloak.tests.client.policies;

import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.CimdProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCimdProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.tests.oauth.AbstractJWTAuthorizationGrantTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = ClientIdMetadataDocumentJwtGrantDisabledTest.JwtAuthorizationGrantDisabledServerConfig.class)
public class ClientIdMetadataDocumentJwtGrantDisabledTest {

    private static final String CLIENT_ID = "http://localhost:8500/cimd/metadata";
    private static final String REDIRECT_URI = "http://localhost:8500/";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = AbstractJWTAuthorizationGrantTest.FederatedUserConfiguration.class)
    ManagedUser user;

    @InjectCimdProvider(config = ClientIdMetadataDocumentTest.CimdClientConfig.class, lifecycle = LifeCycle.METHOD)
    CimdProvider cimd;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    OAuthGrantPage grantPage;

    @Test
    public void testAcceptPublicClientWithJwtGrantWhenFeatureDisabled() {
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setAcceptPublicClientWithConfidentialClientOnlyGrant(true);
        updatePolicy(conditionConfig, executorConfig);

        cimd.getRepresentation().setTokenEndpointAuthMethod(null);
        cimd.getRepresentation().setJwksUri(null);
        cimd.getRepresentation().setGrantTypes(List.of(
                OAuth2Constants.AUTHORIZATION_CODE,
                OAuth2Constants.JWT_AUTHORIZATION_GRANT
        ));

        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).open();
        oauth.fillLoginForm(user.getUsername(), user.getPassword());
        grantPage.assertCurrent();
        grantPage.accept();
        String code = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);

        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(CLIENT_ID);
        Assertions.assertEquals(1, clients.size());
        ClientRepresentation client = clients.get(0);
        Assertions.assertTrue(client.isPublicClient());
        Assertions.assertTrue(client.isStandardFlowEnabled());
        Map<String, String> attrs = client.getAttributes();
        Assertions.assertNotEquals("true", attrs.get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));

        oauth.logoutRequest().idTokenHint(tokenResponse.getIdToken()).send();
        realm.admin().clients().get(client.getId()).remove();
    }

    private void updatePolicy(ClientIdUriSchemeCondition.Configuration conditionConfig,
            ClientIdMetadataDocumentExecutor.Configuration executorConfig) {
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig)
                    .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, conditionConfig)
                    .profile("executor")
                    .build());
            return r;
        });
    }

    public static class JwtAuthorizationGrantDisabledServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CIMD, Profile.Feature.RESOURCE_INDICATORS)
                    .featuresDisabled(Profile.Feature.JWT_AUTHORIZATION_GRANT);
        }
    }
}
