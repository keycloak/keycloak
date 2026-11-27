package org.keycloak.tests.broker;

import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.IdpLinkAction;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.broker.oidc.TestKeycloakOidcIdentityProviderFactory;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerLinkReplacementTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = LinkReplacementRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectPage
    ErrorPage errorPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @DatabaseTest
    @ParameterizedTest
    @CsvSource({"false, true", "true, false", "true, true"})
    public void linkingDifferentExternalAccount(boolean allowReplacement, boolean issueRefreshToken) {
        if (allowReplacement) {
            IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
            IdentityProviderRepresentation config = idp.toRepresentation();
            config.getConfig().put(IdentityProviderModel.ALLOW_LINKING_WITH_EXISTING_FEDERATED_IDENTITY, "true");
            idp.update(config);
        }
        if (!issueRefreshToken) {
            var clients = providerRealm.admin().clients();
            ClientRepresentation brokerClient = clients.findByClientId(CLIENT_ID).get(0);
            brokerClient.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
            clients.get(brokerClient.getId()).update(brokerClient);
        }

        UserRepresentation localUser = UserBuilder.create("local-user")
                .firstName("Local").lastName("User").email("local-user@example.org").emailVerified(true)
                .enabled(true).password(USER_PASSWORD)
                .build();
        String localUserId = ApiUtil.getCreatedId(consumerRealm.admin().users().create(localUser));
        UserResource localUserResource = consumerRealm.admin().users().get(localUserId);

        FederatedIdentityRepresentation previousLink = new FederatedIdentityRepresentation();
        previousLink.setIdentityProvider(getIdpAlias());
        previousLink.setUserId("previous-provider-user-id");
        previousLink.setUserName("previous-provider-username");
        try (Response response = localUserResource.addFederatedIdentity(getIdpAlias(), previousLink)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        oauth.openLoginForm();
        loginPage.fillLogin(localUser.getUsername(), USER_PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess());

        String realmName = consumerRealm.getName();
        String providerAlias = getIdpAlias();
        String refreshToken = AbstractOAuth2IdentityProvider.FEDERATED_REFRESH_TOKEN;
        String providerNote = refreshToken + ":" + providerAlias;
        String otherProviderNote = refreshToken + ":other-provider";
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserById(realm, localUserId);
            UserSessionModel currentSession = session.sessions().getUserSessionsStream(realm, user).findFirst().orElseThrow();
            currentSession.setNote(UserAuthenticationIdentityProvider.EXTERNAL_IDENTITY_PROVIDER, "other-provider");
            UserSessionModel otherSession = session.sessions().createUserSession(null, realm, user, user.getUsername(),
                    "127.0.0.1", "openid-connect", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            otherSession.setNote(Details.IDENTITY_PROVIDER, providerAlias);
            for (UserSessionModel userSession : List.of(currentSession, otherSession)) {
                userSession.setNote(providerNote, "previous-token");
                userSession.setNote(refreshToken, "legacy-token");
                userSession.setNote(otherProviderNote, "other-token");
            }
            session.sessions().createOfflineUserSession(otherSession);
        });

        oauth.loginForm().kcAction(IdpLinkAction.PROVIDER_ID + ":" + getIdpAlias()).open();
        webDriver.findElement(By.id("kc-continue")).click();
        logInAsUserInIDPForFirstTime();

        List<FederatedIdentityRepresentation> links = localUserResource.getFederatedIdentity();
        assertEquals(1, links.size());
        FederatedIdentityRepresentation storedLink = links.get(0);
        assertEquals(getIdpAlias(), storedLink.getIdentityProvider());
        if (allowReplacement) {
            assertEquals("success", oauth.parseLoginResponse().getKcActionStatus());
            String providerUserId = providerRealm.admin().users().search(getUserLogin(), true).get(0).getId();
            assertEquals(providerUserId, storedLink.getUserId());
            assertEquals(getUserLogin(), storedLink.getUserName());
        } else {
            assertEquals("Your account is already linked to the identity provider " + getIdpAlias() + ".", errorPage.getError());
            assertEquals(previousLink.getUserId(), storedLink.getUserId());
            assertEquals(previousLink.getUserName(), storedLink.getUserName());
        }

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserById(realm, localUserId);
            var online = session.sessions().getUserSessionsStream(realm, user).toList();
            var offline = session.sessions().getOfflineUserSessionsStream(realm, user).toList();
            assertEquals(2, online.size());
            assertEquals(1, offline.size());
            Stream.concat(online.stream(), offline.stream()).forEach(userSession -> {
                boolean current = "other-provider".equals(userSession.getNote(UserAuthenticationIdentityProvider.EXTERNAL_IDENTITY_PROVIDER));
                String storedRefreshToken = userSession.getNote(providerNote);
                if (allowReplacement && current && issueRefreshToken) {
                    assertTrue(storedRefreshToken != null && !"previous-token".equals(storedRefreshToken), "Expected a new refresh token");
                } else {
                    assertTrue(allowReplacement ? storedRefreshToken == null : "previous-token".equals(storedRefreshToken),
                            "Unexpected refresh token in session");
                }
                boolean legacyNotesCleared = allowReplacement && providerAlias.equals(userSession.getNote(Details.IDENTITY_PROVIDER));
                if (current) {
                    assertNull(userSession.getNote(Details.IDENTITY_PROVIDER));
                }
                assertEquals(legacyNotesCleared ? null : "legacy-token", userSession.getNote(refreshToken));
                assertEquals("other-token", userSession.getNote(otherProviderNote));
            });
        });
    }

    static class LinkReplacementRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm, createOidcIdentityProvider()
                    .providerId(TestKeycloakOidcIdentityProviderFactory.ID)
                    .attribute(TestKeycloakOidcIdentityProviderFactory.LEGACY_ID, "incoming-legacy-id"));
        }
    }
}
