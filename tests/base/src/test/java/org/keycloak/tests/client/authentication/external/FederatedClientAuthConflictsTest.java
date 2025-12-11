package org.keycloak.tests.client.authentication.external;

import java.util.UUID;

import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = ClientAuthIdpServerConfig.class)
public class FederatedClientAuthConflictsTest {

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectEvents
    Events events;

    @Test
    public void testDuplicatedIssuers() {
        createIdp("idp1", "http://127.0.0.1:8500");
        createIdp("idp2", "http://127.0.0.1:8500");

        ClientRepresentation clientRep = createClient("myclient", "external1", "idp1");

        // Should pass as the first matching IdP by alias is always used
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(createDefaultToken("external1", "http://127.0.0.1:8500")).send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("myclient", events.poll().getClientId());

        clientRep.getAttributes().put(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, "idp2");

        realm.admin().clients().get(clientRep.getId()).update(clientRep);

        // Should fail since it's using the second IdP
        response = oAuthClient.clientCredentialsGrantRequest().clientJwt(createDefaultToken("external1", "http://127.0.0.1:8500")).send();
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertNull(events.poll().getClientId());
    }

    @Test
    public void testDuplicatedClientExternalId() {
        createIdp("idp1", "http://127.0.0.1:8500/one");
        createIdp("idp2", "http://127.0.0.1:8500/two");

        createClient("myclient1", "external1", "idp1");
        createClient("myclient2", "external1", "idp2");

        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(createDefaultToken("external1", "http://127.0.0.1:8500/one")).send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("myclient1", events.poll().getClientId());

        response = oAuthClient.clientCredentialsGrantRequest().clientJwt(createDefaultToken("external1", "http://127.0.0.1:8500/two")).send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("myclient2", events.poll().getClientId());
    }

    private String createDefaultToken(String externalClientId, String issuer) {
        JsonWebToken token = new JsonWebToken();
        token.id(UUID.randomUUID().toString());
        token.issuer(issuer);
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.exp((long) (Time.currentTime() + 300));
        token.subject(externalClientId);
        return identityProvider.encodeToken(token);
    }

    private IdentityProviderRepresentation createIdp(String alias, String issuer) {
        IdentityProviderRepresentation rep = IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(alias)
                .setAttribute(IdentityProviderModel.ISSUER, issuer)
                .setAttribute(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS, "true")
                .setAttribute(OIDCIdentityProviderConfig.USE_JWKS_URL, "true")
                .setAttribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                .build();
        rep.setInternalId(ApiUtil.getCreatedId(realm.admin().identityProviders().create(rep)));
        return rep;
    }

    private ClientRepresentation createClient(String clientId, String externalClientId, String idpAlias) {
        ClientRepresentation rep = ClientConfigBuilder.create().clientId(clientId)
                .serviceAccountsEnabled(true)
                .authenticatorType(FederatedJWTClientAuthenticator.PROVIDER_ID)
                .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, idpAlias)
                .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, externalClientId)
                .build();
        rep.setId(ApiUtil.getCreatedId(realm.admin().clients().create(rep)));
        return rep;
    }

}
