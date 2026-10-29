package org.keycloak.tests.broker.oidc;

import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerTest extends AbstractKcOidcBrokerTest {

    @InjectPage
    ErrorPage errorPage;

    @InjectEvents(realmRef = "consumer")
    Events events;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    private static String activeRs256PublicKey(ManagedRealm realm) {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> k.getPublicKey() != null && KeyUse.SIG.equals(k.getUse())
                        && Algorithm.RS256.equals(k.getAlgorithm()))
                .map(KeyMetadataRepresentation::getPublicKey)
                .findFirst().orElseThrow();
    }

    @Test
    public void testTrustEmailWhenEmailVerifiedClaimIsMissingFromUserInfo() {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.setTrustEmail(true);
        idp.getConfig().put("disableUserInfo", "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        // Send the email only through UserInfo and omit email_verified.
        ClientScopeResource emailScope = AdminApiUtil.findClientScopeByName(providerRealm.admin(), "email");
        emailScope.getProtocolMappers().getMappers().forEach(mapper -> {
            if (IDToken.EMAIL_VERIFIED.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                emailScope.getProtocolMappers().delete(mapper.getId());
            } else if (IDToken.EMAIL.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
                emailScope.getProtocolMappers().update(mapper.getId(), mapper);
            }
        });
        ClientResource providerClient = providerRealm.admin().clients().get(
                providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId());
        providerClient.getProtocolMappers().getMappers().forEach(mapper -> {
            if (IDToken.EMAIL.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
                providerClient.getProtocolMappers().update(mapper.getId(), mapper);
            }
        });

        disableUpdateProfileOnFirstLogin();
        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());
        UserRepresentation consumerUser = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin());
        assertEquals(getUserEmail(), consumerUser.getEmail());
        assertTrue(consumerUser.isEmailVerified());
    }

    @Test
    public void testIdpCreatedWithoutValidateSignature_LoginFailsWhenTokenSignedWithUntrustedKey() {
        String providerPublicKey = activeRs256PublicKey(providerRealm);
        String consumerPublicKey = activeRs256PublicKey(consumerRealm);

        recreateIdpWithoutValidateSignature(providerPublicKey);

        // Control: with the trusted key the login succeeds, so the failure below can only be caused by the signature
        disableUpdateProfileOnFirstLogin();
        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());

        AccountHelper.logout(consumerRealm.admin(), getUserLogin());
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        events.clear();

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, consumerPublicKey);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        assertFalse(consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation().getConfig().containsKey(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE));

        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_LOGIN_ERROR)
                .error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
    }

    private void recreateIdpWithoutValidateSignature(String publicKey) {
        IdentityProviderRepresentation idp = fixtureIdpWithoutValidateSignature(publicKey);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).remove();

        try (Response response = consumerRealm.admin().identityProviders().create(idp)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        assertFalse(consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation().getConfig().containsKey(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE));
    }

    // The fixture IdP without validateSignature, verifying with the given public key (or with no verification key if null)
    private IdentityProviderRepresentation fixtureIdpWithoutValidateSignature(String publicKey) {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.setInternalId(null);
        Map<String, String> config = idp.getConfig();
        config.remove(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE);
        config.put(OIDCIdentityProviderConfig.USE_JWKS_URL, "false");
        if (publicKey != null) {
            config.put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, publicKey);
        }
        // The secret is masked when read back from the admin API
        config.put("clientSecret", getBrokerClientSecret());
        return idp;
    }

    // POST /admin/realms is a full realm import, which is different from the metadata import-config endpoint
    private RealmRepresentation realmWithIdpWithoutValidateSignature(String realmName, String publicKey) {
        return RealmBuilder.create()
                .name(realmName)
                .clients(ClientBuilder.create(CONSUMER_BROKER_APP_CLIENT_ID)
                        .secret(CONSUMER_BROKER_APP_SECRET)
                        .redirectUris(oauth.getRedirectUri()))
                .identityProviders(fixtureIdpWithoutValidateSignature(publicKey))
                .build();
    }

    @Test
    public void testRealmImportWithoutValidateSignature_LoginFailsWhenTokenSignedWithUntrustedKey() {
        String realmName = "imported-consumer";
        String providerPublicKey = activeRs256PublicKey(providerRealm);
        String consumerPublicKey = activeRs256PublicKey(consumerRealm);

        // The provider client must accept the broker endpoint of the imported realm
        String importedBroker = consumerRealm.getBaseUrl().replace("/realms/" + CONSUMER_REALM, "/realms/" + realmName) + "/broker/" + getIdpAlias() + "/endpoint";
        ClientRepresentation providerClient = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0);
        providerClient.getRedirectUris().add(importedBroker + "/*");
        providerRealm.admin().clients().get(providerClient.getId()).update(providerClient);

        adminClient.realms().create(realmWithIdpWithoutValidateSignature(realmName, consumerPublicKey));
        try {
            IdentityProviderRepresentation imported = adminClient.realm(realmName).identityProviders().get(getIdpAlias()).toRepresentation();
            assertFalse(imported.getConfig().containsKey(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE));

            oauth.realm(realmName);
            oauth.openLoginForm();
            logInWithBroker();
            logInAsUserInIDPForFirstTime();

            errorPage.assertCurrent();

            // Control: with the trusted key the same login succeeds, so the failure above can only be caused by the signature
            imported.getConfig().put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, providerPublicKey);
            adminClient.realm(realmName).identityProviders().get(getIdpAlias()).update(imported);
            assertFalse(adminClient.realm(realmName).identityProviders().get(getIdpAlias()).toRepresentation().getConfig().containsKey(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE));
            AccountHelper.logout(providerRealm.admin(), getUserLogin());

            oauth.openLoginForm();
            logInWithBroker();
            logInAsUserInIDPForFirstTime();
            updateAccountInformation();
            assertTrue(oauth.parseLoginResponse().isSuccess());
        } finally {
            oauth.realm(CONSUMER_REALM);
            adminClient.realm(realmName).remove();
        }
    }

    @Test
    public void testRealmImportWithoutValidateSignatureAndVerificationKeyIsRejected() {
        String realmName = "imported-invalid-consumer";

        // With validateSignature absent, signature validation is enabled, but there is neither a JWKS URL nor a public key to verify with
        RealmRepresentation invalid = realmWithIdpWithoutValidateSignature(realmName, null);
        assertFalse(invalid.getIdentityProviders().get(0).getConfig().containsKey(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE));
        assertThrows(WebApplicationException.class, () -> adminClient.realms().create(invalid));
        assertThrows(NotFoundException.class, () -> adminClient.realm(realmName).toRepresentation());

        // Control: the same realm is imported when signature validation is explicitly disabled
        RealmRepresentation valid = realmWithIdpWithoutValidateSignature(realmName, null);
        valid.getIdentityProviders().get(0).getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "false");
        adminClient.realms().create(valid);
        adminClient.realm(realmName).remove();
    }

    @Test
    public void testLoginFailsWhenToken_SignedWithUntrustedKeyAND_ValidateSignatureIsExplicitlyTrue() {
        String providerPublicKey = activeRs256PublicKey(providerRealm);
        String consumerPublicKey = activeRs256PublicKey(consumerRealm);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        Map<String, String> config = idp.getConfig();

        config.put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        config.put(OIDCIdentityProviderConfig.USE_JWKS_URL, "false");
        config.put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, providerPublicKey);

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        disableUpdateProfileOnFirstLogin();
        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());

        AccountHelper.logout(consumerRealm.admin(), getUserLogin());
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        events.clear();

        idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        config = idp.getConfig();
        config.put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, consumerPublicKey);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_LOGIN_ERROR)
                .error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
    }
}
