package org.keycloak.testsuite.broker;

import java.io.Closeable;
import java.io.IOException;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public final class KcSamlBrokerArtifactBindingTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testLoginNoSignatures() throws Exception {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL,
                        BrokerTestTools.getProviderRoot() + "/auth/realms/" + bc.providerRealmName() + "/protocol/saml/resolve")
                .setAttribute(SAMLIdentityProviderConfig.ARTIFACT_BINDING_RESPONSE, Boolean.TRUE.toString())
                .update();
            Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, Boolean.TRUE.toString())
                .update()) {
            login(true);
        }
    }

    @Test
    public void testLoginWithSignatureAtResponseLevelSuccess() throws Exception {
        // success just with signature at response level
        performTestSuccess(true, false, false,
                false, false);
    }

    @Test
    public void testLoginErrorNoSignature() throws Exception {
        // failure with no signature at all
        performTestFailure(false, false, false,
                false, false);
    }

    @Test
    public void testLoginWithSignatureAtResponseLevelErrorInvalidSignature() throws Exception {
        // failure with invalid signature at response level
        performTestFailureInvalidCert(true, false, false,
                false, false);
    }

    @Test
    public void testLoginWithSignatureAtAssertionLevelSuccess() throws Exception {
        // success with signature at assertion level
        performTestSuccess(false, true, false,
                false, false);
    }

    @Test
    public void testLoginWithSignatureAtAssertionLevelWantAssertionSignatureSuccess() throws Exception {
        // success with signature at assertion level and wanting assertion signed
        performTestSuccess(false, true, false,
                true, false);
    }

    @Test
    public void testLoginWithSignatureAtAssertionLevelErrorOnlySignatureAtResponseLevel() throws Exception {
        // failure when signature in response level only but wanting assertion signed
        performTestFailure(true, false, false,
                true, false);
    }

    @Test
    public void testLoginWithSignatureAtAssertionLevelErrorInvalidSignature() throws Exception {
        // failure when assertion level signature with invalid cert
        performTestFailureInvalidCert(false, true, false,
                false, false);
    }

    @Test
    public void testLoginWithSignatureAndEncryptionAtAssertionLevelSuccess() throws Exception {
        // success with assertion encrypted and signed
        performTestSuccess(false, true, true,
                true, true);
    }

    @Test
    public void testLoginWithOnlyEncryptionAtAssertionLevelErrorNoSignature() throws Exception {
        // failure with assertion encrypted but not signed
        performTestFailure(false, false, true,
                false, false);
    }

    private void performTestSuccess(boolean providerSignsResponse, boolean providerSignsAssertion, boolean providerEncryptsAssertion,
            boolean consumerWantsAssertionSigned, boolean consumerWantsAssertionEncrypted) throws IOException {
        performTest(providerSignsResponse, providerSignsAssertion, providerEncryptsAssertion,
                consumerWantsAssertionSigned, consumerWantsAssertionEncrypted,
                false, true);
    }

    private void performTestFailure(boolean providerSignsResponse, boolean providerSignsAssertion, boolean providerEncryptsAssertion,
            boolean consumerWantsAssertionSigned, boolean consumerWantsAssertionEncrypted) throws IOException {
        performTest(providerSignsResponse, providerSignsAssertion, providerEncryptsAssertion,
                consumerWantsAssertionSigned, consumerWantsAssertionEncrypted,
                false, false);
    }

    private void performTestFailureInvalidCert(boolean providerSignsResponse, boolean providerSignsAssertion, boolean providerEncryptsAssertion,
            boolean consumerWantsAssertionSigned, boolean consumerWantsAssertionEncrypted) throws IOException {
        performTest(providerSignsResponse, providerSignsAssertion, providerEncryptsAssertion,
                consumerWantsAssertionSigned, consumerWantsAssertionEncrypted,
                true, false);
    }

    private void performTest(boolean providerSignsResponse, boolean providerSignsAssertion, boolean providerEncryptsAssertion,
            boolean consumerWantsAssertionSigned, boolean consumerWantsAssertionEncrypted,
            boolean incorrectSigningKey, boolean success) throws IOException {
        String consumerSigCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RS256).getCertificate();
        MatcherAssert.assertThat(consumerSigCert, Matchers.notNullValue());
        String consumerEncCert = KeyUtils.findActiveEncryptingKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RSA_OAEP).getCertificate();
        MatcherAssert.assertThat(consumerEncCert, Matchers.notNullValue());

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL,
                        BrokerTestTools.getProviderRoot() + "/auth/realms/" + bc.providerRealmName() + "/protocol/saml/resolve")
                .setAttribute(SAMLIdentityProviderConfig.ARTIFACT_BINDING_RESPONSE, Boolean.TRUE.toString())
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, Boolean.TRUE.toString()) // always sign requests
                .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString()) // always validate signatures
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, Boolean.toString(consumerWantsAssertionSigned))
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, Boolean.toString(consumerWantsAssertionEncrypted))
                .setAttribute(SAMLIdentityProviderConfig.METADATA_DESCRIPTOR_URL,
                        BrokerTestTools.getProviderRoot() + "/auth/realms/" + bc.providerRealmName() + "/protocol/saml/descriptor")
                .setAttribute(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, Boolean.toString(!incorrectSigningKey)) // use metadata only if correct cert
                .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, consumerSigCert)
                .update();
            Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_ARTIFACT_BINDING, Boolean.TRUE.toString())
                .setAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, consumerSigCert)
                .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.TRUE.toString())
                .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.toString(providerSignsResponse))
                .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.toString(providerSignsAssertion))
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(providerEncryptsAssertion))
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, consumerEncCert)
                .update()) {
            login(success);
        }
    }

    private void login(boolean success) {
        // login using artifact binding
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        if (success) {
            updateAccountInformationPage.assertCurrent();
            updateAccountInformationPage.updateAccountInformation("f", "l");
            appPage.assertCurrent();
        } else {
            errorPage.assertCurrent();
            Assert.assertEquals("Invalid signature in response from identity provider.", errorPage.getError());
        }
    }
}
