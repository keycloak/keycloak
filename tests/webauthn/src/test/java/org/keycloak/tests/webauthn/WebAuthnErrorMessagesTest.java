package org.keycloak.tests.webauthn;

import org.keycloak.WebAuthnConstants;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests that some server side webauthn4j verification failures are reported with a specific,
 * localizable message key
 */
@KeycloakIntegrationTest
public class WebAuthnErrorMessagesTest extends AbstractWebAuthnVirtualTest {

    private static final String FOREIGN_ORIGIN = "https://not-the-right-origin.example.com";
    private static final String RANDOM_CHALLENGE = "opts.publicKey.challenge = new Uint8Array(32).fill(7);";

    // Registration

    @Test
    public void registrationBadChallenge() {
        registerAndExpectError("bad-challenge",
                tamperCreateOptions(RANDOM_CHALLENGE),
                Messages.WEBAUTHN_ERROR_BAD_CHALLENGE);

        assertThat(webAuthnErrorPage.getError(), containsString("Passkey challenge mismatch or expired."));
    }

    @Test
    public void registrationBadOrigin() {
        registerAndExpectError("bad-origin",
                tamperClientDataOrigin("register", FOREIGN_ORIGIN),
                Messages.WEBAUTHN_ERROR_BAD_ORIGIN);
    }

    @Test
    public void registrationUserNotVerified() {
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyUserVerificationRequirement("required"));

        registerAndExpectError("user-not-verified",
                tamperCreateOptions(
                        "opts.publicKey.authenticatorSelection = opts.publicKey.authenticatorSelection || {};" +
                                "opts.publicKey.authenticatorSelection.userVerification = 'discouraged';"),
                Messages.WEBAUTHN_ERROR_USER_NOT_VERIFIED);
    }

    @Test
    public void registrationUnmappedErrorFallsBackToGenericKey() {
        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyAttestationConveyancePreference("direct"));

        registerAndExpectError("unmapped",
                tamperCreateOptions("opts.publicKey.attestation = 'none';"),
                Messages.WEBAUTHN_ERROR_REGISTRATION);
    }

    // Authentication

    @Test
    public void authenticationBadChallenge() {
        String username = registerUserWithPasskey("auth-bad-challenge");

        loginAndExpectError(username,
                tamperGetOptions(RANDOM_CHALLENGE),
                Messages.WEBAUTHN_ERROR_BAD_CHALLENGE);
    }

    @Test
    public void authenticationBadOrigin() {
        String username = registerUserWithPasskey("auth-bad-origin");

        loginAndExpectError(username,
                tamperClientDataOrigin("webauth", FOREIGN_ORIGIN),
                Messages.WEBAUTHN_ERROR_BAD_ORIGIN);
    }

    @Test
    public void authenticationBadRpId() {
        String username = registerUserWithPasskey("auth-bad-rpid");

        loginAndExpectError(username,
                tamperBytes("authenticatorData", "bytes[0] ^= 0xFF;"),
                Messages.WEBAUTHN_ERROR_BAD_RPID);
    }

    @Test
    public void authenticationUserNotPresent() {
        String username = registerUserWithPasskey("auth-user-not-present");

        loginAndExpectError(username,
                tamperBytes("authenticatorData", "bytes[32] &= 0xFE;"),
                Messages.WEBAUTHN_ERROR_USER_NOT_PRESENT);
    }

    @Test
    public void authenticationUserNotVerified() {
        String username = registerUserWithPasskey("auth-user-not-verified");

        managedRealm.updateWithCleanup(r -> r
                .webAuthnPolicyUserVerificationRequirement("required"));

        loginAndExpectError(username,
                tamperGetOptions("opts.publicKey.userVerification = 'discouraged';"),
                Messages.WEBAUTHN_ERROR_USER_NOT_VERIFIED);
    }

    @Test
    public void authenticationBadSignature() {
        String username = registerUserWithPasskey("auth-bad-signature");

        loginAndExpectError(username,
                tamperBytes("signature", "bytes[bytes.length - 1] ^= 0xFF;"),
                Messages.WEBAUTHN_ERROR_BAD_SIGNATURE);

        assertThat(webAuthnErrorPage.getError(), containsString("Passkey signature verification failed."));
    }

    private void registerAndExpectError(String testId, String tamperScript, String expectedMessageKey) {
        String username = "error-" + testId;
        String email = "error-" + testId + "@email";

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, PASSWORD);

        webAuthnRegisterPage.assertCurrent();

        if (tamperScript != null) {
            ((JavascriptExecutor) driver.driver()).executeScript(tamperScript);
        }

        events.clear();
        webAuthnRegisterPage.clickRegister();

        if (webAuthnRegisterPage.isRegisterAlertPresent()) {
            webAuthnRegisterPage.registerWebAuthnCredential(SecretGenerator.getInstance().randomString(24));
        }

        webAuthnErrorPage.assertCurrent();
        assertErrorEvent(WebAuthnConstants.REG_ERR_LABEL, expectedMessageKey, Errors.INVALID_REGISTRATION);
    }

    private String registerUserWithPasskey(String username) {
        registerUser(username, PASSWORD, username + "@email",
                SecretGenerator.getInstance().randomString(24), true);

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        logout();

        return username;
    }

    private void loginAndExpectError(String username, String tamperScript, String expectedMessageKey) {
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(username, PASSWORD);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();

        if (tamperScript != null) {
            ((JavascriptExecutor) driver.driver()).executeScript(tamperScript);
        }

        events.clear();
        webAuthnLoginPage.clickAuthenticate();

        webAuthnErrorPage.assertCurrent();
        assertErrorEvent(WebAuthnConstants.AUTH_ERR_LABEL, expectedMessageKey, Errors.INVALID_USER_CREDENTIALS);
    }

    private void assertErrorEvent(String errorLabel, String expectedMessageKey, String expectedError) {
        EventRepresentation event = null;

        for (int i = 0; i < 10 && event == null; i++) {
            EventRepresentation polled = events.poll();
            if (polled == null) {
                break;
            }
            if (polled.getDetails() != null && polled.getDetails().containsKey(errorLabel)) {
                event = polled;
            }
        }

        Assertions.assertNotNull(event, "No error event with detail '" + errorLabel + "' was found");

        EventAssertion.assertError(event)
                .error(expectedError)
                .details(errorLabel, expectedMessageKey);
    }

    // JS tampering

    private static final String BASE64URL_HELPERS =
            "window.kcToBytes = function(value) {" +
                    "  const raw = atob(value.replace(/-/g, '+').replace(/_/g, '/'));" +
                    "  const bytes = new Uint8Array(raw.length);" +
                    "  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);" +
                    "  return bytes;" +
                    "};" +
                    "window.kcToBase64Url = function(bytes) {" +
                    "  let raw = '';" +
                    "  for (let i = 0; i < bytes.length; i++) raw += String.fromCharCode(bytes[i]);" +
                    "  return btoa(raw).replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/, '');" +
                    "};";

    private static String tamperCreateOptions(String body) {
        return "const origCreate = navigator.credentials.create.bind(navigator.credentials);" +
                "navigator.credentials.create = function(opts) {" +
                "  " + body +
                "  return origCreate(opts);" +
                "};";
    }

    private static String tamperGetOptions(String body) {
        return "const origGet = navigator.credentials.get.bind(navigator.credentials);" +
                "navigator.credentials.get = function(opts) {" +
                "  " + body +
                "  return origGet(opts);" +
                "};";
    }

    /**
     * Rewrites a base64url encoded byte field just before the assertion form is submitted.
     */
    private static String tamperBytes(String fieldId, String mutation) {
        return BASE64URL_HELPERS +
                "document.getElementById('webauth').addEventListener('submit', function() {" +
                "  const field = document.getElementById('" + fieldId + "');" +
                "  const bytes = window.kcToBytes(field.value);" +
                "  " + mutation +
                "  field.value = window.kcToBase64Url(bytes);" +
                "});";
    }

    /**
     * Rewrites the origin inside clientDataJSON just before the given form is submitted.
     */
    private static String tamperClientDataOrigin(String formId, String origin) {
        return BASE64URL_HELPERS +
                "document.getElementById('" + formId + "').addEventListener('submit', function() {" +
                "  const field = document.getElementById('clientDataJSON');" +
                "  const clientData = JSON.parse(new TextDecoder().decode(window.kcToBytes(field.value)));" +
                "  clientData.origin = '" + origin + "';" +
                "  field.value = window.kcToBase64Url(new TextEncoder().encode(JSON.stringify(clientData)));" +
                "});";
    }
}
