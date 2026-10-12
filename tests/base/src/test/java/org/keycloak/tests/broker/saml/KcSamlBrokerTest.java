package org.keycloak.tests.broker.saml;

import java.util.Map;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

    @InjectPage
    ErrorPage errorPage;

    @InjectEvents(realmRef = "consumer")
    Events events;

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
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, activeRs256SigCert(providerRealm));

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        // Attempt to log in should fail due to missing signature
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenAssertionHasInvalidSignatureAndWantAssertionsSignedIsTrue() {
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
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsOmitted() {
        // wantAssertionsSigned defaults to true when omitted.
        configureSignedResponseWithUnsignedAssertion(true, true);
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginSucceedsWhenWantAssertionsSignedIsFalse() {
        configureSignedResponseWithUnsignedAssertion(false,true);
        loginUser();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsTrueAndValidateSignatureIsFalse() {
        configureSignedResponseWithUnsignedAssertion(true, false);
        assertLoginFailsWithSignatureError();
    }

    private void assertLoginFailsWithSignatureError() {
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                .error(Errors.INVALID_SIGNATURE);
    }


    private void configureSignedResponseWithUnsignedAssertion(
            Boolean wantAssertionsSigned,
            boolean validateSignature) {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients()
                .findByClientId(samlClientId).get(0);

        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp =
                consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE,
                Boolean.toString(validateSignature));
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY,
                activeRs256SigCert(providerRealm));

        if (wantAssertionsSigned == null) {
            config.remove(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED);
        } else {
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED,
                    Boolean.toString(wantAssertionsSigned));
        }

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}
