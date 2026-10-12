package org.keycloak.tests.broker.saml;

import java.util.Map;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

    @InjectPage
    ErrorPage errorPage;

    @Test
    public void testValidateSignature_defaultsToTrueWhenNOTConfigured() {
        SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig();
        Assertions.assertTrue(config.isValidateSignature());
        Assertions.assertTrue(config.isWantAssertionsSigned());
    }

    @Test
    public void testLoginFailsWhenResponse_UnsignedANDValidateSignatureIsOmitted() {
        // Configure the provider
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.remove(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE);

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        // Attempt to log in should fail due to missing signature
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        Assertions.assertTrue(errorPage.getError().contains("invalid")
                || errorPage.getError().contains("signature")
                || errorPage.getError().contains("authenticating"),
                "Expected signature validation error but got: " + errorPage.getError());
    }

    @Test
    public void testLoginFailWhenAssertionHas_InvalidSignatureAND_isWantAssertionsSignedIsTrue() {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        providerRealm.admin().clients().get(client.getId()).update(client);

        String consumerSigningCert = activeRs256SigCert(consumerRealm);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, consumerSigningCert);
        config.remove(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE);

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        // Assert that login failed due to signature validation error
        errorPage.assertCurrent();
        Assertions.assertTrue(errorPage.getError().contains("invalid")
                || errorPage.getError().contains("signature")
                || errorPage.getError().contains("authenticating"),
                "Expected signature validation error but got: " + errorPage.getError());
    }
}
