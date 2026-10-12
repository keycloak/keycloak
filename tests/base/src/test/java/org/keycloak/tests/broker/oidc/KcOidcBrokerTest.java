package org.keycloak.tests.broker.oidc;

import java.util.Map;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerTest extends AbstractKcOidcBrokerTest {

    @InjectPage
    ErrorPage errorPage;

    @InjectEvents(realmRef = "consumer")
    Events events;

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
    public void testLoginFailsWhenToken_SignedWithUntrustedKeyAND_ValidateSignatureIsOmitted() {
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
