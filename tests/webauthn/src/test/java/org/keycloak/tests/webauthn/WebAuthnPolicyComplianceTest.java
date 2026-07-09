package org.keycloak.tests.webauthn;

import java.util.List;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import static org.keycloak.WebAuthnConstants.AUTHENTICATOR_ATTACHMENT_CROSS_PLATFORM;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests that WebAuthn registration enforces realm policy server-side.
 *
 * Most policy constraints (signature algorithm, authenticator attachment, excludeCredentials)
 * are enforced client-side by the browser before the credential reaches the server,
 * so they cannot be tested with virtual authenticators via the normal flow.
 *
 * To test server-side attachment validation, we inject JS to tamper the form field
 * before submission — simulating a malicious client that bypasses browser constraints.
 */
@KeycloakIntegrationTest
public class WebAuthnPolicyComplianceTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void tamperedAuthenticatorAttachment() {
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAuthenticatorAttachment(AUTHENTICATOR_ATTACHMENT_CROSS_PLATFORM));

        registerAndExpectError("attach-tamper",
                tamperFormField("authenticatorAttachment", "platform"),
                "Policy requires 'cross-platform' authenticator attachment but got 'platform'");
    }

    @Test
    public void invalidAuthenticatorAttachmentValue() {
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAuthenticatorAttachment(AUTHENTICATOR_ATTACHMENT_CROSS_PLATFORM));

        registerAndExpectError("attach-invalid",
                tamperFormField("authenticatorAttachment", "not-a-real-value"),
                "Unexpected authenticator attachment value");
    }

    @Test
    public void tamperedSignatureAlgorithm() {
        // Policy: ES512 only. Tamper pubKeyCredParams to ES256.
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicySignatureAlgorithms(List.of("ES512")));

        registerAndExpectError("alg-tamper",
                tamperCreateOptions("opts.publicKey.pubKeyCredParams = [{type: 'public-key', alg: -7}];"),
                "alg not listed in options.pubKeyCredParams is used.");
    }

    @Test
    public void tamperedUserVerification() {
        // Policy: UV required. Tamper to "discouraged" — default virtual authenticator has isUserVerified=false.
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyUserVerificationRequirement("required"));

        registerAndExpectError("uv-tamper",
                tamperCreateOptions(
                        "opts.publicKey.authenticatorSelection = opts.publicKey.authenticatorSelection || {};" +
                        "opts.publicKey.authenticatorSelection.userVerification = 'discouraged';"),
                "Verifier is configured to check user verified, but UV flag in authenticatorData is not set.");
    }

    @Test
    public void tamperedAttestationConveyance() {
        // Policy: "direct" attestation. Tamper to "none" — server has no verifier for fmt:"none".
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAttestationConveyancePreference("direct"));

        registerAndExpectError("att-tamper",
                tamperCreateOptions("opts.publicKey.attestation = 'none';"),
                "AttestationVerifier is not configured to handle the supplied AttestationStatement format 'none'.");
    }

    @Test
    public void replayedCredentialRegistration() {
        // Capture form data from a successful registration, then replay it in a new session.
        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", "replay@email", "policy-replay", PASSWORD);

        webAuthnRegisterPage.assertCurrent();

        ((JavascriptExecutor) driver.driver()).executeScript(
                "document.getElementById('register').addEventListener('submit', function() {" +
                "  ['clientDataJSON','attestationObject','publicKeyCredentialId','transports','authenticatorAttachment']" +
                "    .forEach(function(id) { localStorage.setItem('replay_' + id, document.getElementById(id).value); });" +
                "});"
        );

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(SecretGenerator.getInstance().randomString(24));
        logout();

        // Replay the captured credential data in a new registration session
        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", "replay-victim@email", "policy-replay-victim", PASSWORD);

        webAuthnRegisterPage.assertCurrent();

        ((JavascriptExecutor) driver.driver()).executeScript(
                "['clientDataJSON','attestationObject','publicKeyCredentialId','transports','authenticatorAttachment']" +
                "  .forEach(function(id) { document.getElementById(id).value = localStorage.getItem('replay_' + id) || ''; });" +
                "document.getElementById('authenticatorLabel').value = 'replayed';" +
                "document.getElementById('register').requestSubmit();"
        );

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(),
                containsString("The actual challenge does not match the expected challenge"));
    }

    @Test
    public void acceptableAaguidWithNoneAttestation() {
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAcceptableAaguids(List.of(ALL_ZERO_AAGUID))
                .webAuthnPolicyAttestationConveyancePreference("none"));

        registerAndExpectError("aaguid-none-attestation",
                "Acceptable AAGUIDs require an attestation format other than 'none'.");
    }

    private void registerAndExpectError(String testId, String tamperScript, String expectedError) {
        String username = "policy-" + testId;
        String email = "policy-" + testId + "@email";

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, PASSWORD);

        webAuthnRegisterPage.assertCurrent();

        if (tamperScript != null) {
            ((JavascriptExecutor) driver.driver()).executeScript(tamperScript);
        }

        webAuthnRegisterPage.clickRegister();

        if (webAuthnRegisterPage.isRegisterAlertPresent()) {
            webAuthnRegisterPage.registerWebAuthnCredential(SecretGenerator.getInstance().randomString(24));
        }

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(expectedError));
    }

    private void registerAndExpectError(String testId, String expectedError) {
        registerAndExpectError(testId, null, expectedError);
    }

    private static String tamperFormField(String fieldId, String value) {
        return "document.getElementById('register').addEventListener('submit', function() {" +
                "  document.getElementById('" + fieldId + "').value = '" + value + "';" +
                "});";
    }

    private static String tamperCreateOptions(String body) {
        return "const origCreate = navigator.credentials.create.bind(navigator.credentials);" +
                "navigator.credentials.create = function(opts) {" +
                "  " + body +
                "  return origCreate(opts);" +
                "};";
    }
}
