package org.keycloak.tests.webauthn;

import java.util.stream.Stream;

import org.keycloak.WebAuthnConstants;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.services.messages.Messages;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.JavascriptExecutor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Server-side rejection of a missing, empty, or whitespace-only WebAuthn authenticator label.
 * The browser prompt is bypassed by rewriting the hidden form field on submit, because
 * {@code webauthnRegister.js} otherwise re-prompts until a non-blank label is entered.
 */
@KeycloakIntegrationTest
public class WebAuthnRegisterLabelValidationTest extends AbstractWebAuthnVirtualTest {

    static Stream<Arguments> blankAuthenticatorLabels() {
        return Stream.of(
                Arguments.of("missing", null),
                Arguments.of("empty", ""),
                Arguments.of("whitespace", "   ")
        );
    }

    @ParameterizedTest(name = "{0} authenticatorLabel")
    @MethodSource("blankAuthenticatorLabels")
    public void registrationRejectsBlankAuthenticatorLabel(String labelKind, String labelValue) {
        String username = "blank-label-" + (isPasswordless() ? "pwdless-" : "") + labelKind;
        String email = username + "@email";

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, PASSWORD);

        webAuthnRegisterPage.assertCurrent();
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId();
        managedRealm.cleanup().add(r -> r.users().get(userId).remove());

        String challenge = webAuthnRegisterPage.getChallenge();
        assertThat("Challenge should be present in the page", challenge, is(notNullValue()));
        String claimKey = WebAuthnConstants.AUTH_CHALLENGE_NOTE + ":" + challenge;
        assertThat("Single-use object should exist for the issued challenge",
                runOnServer.fetch(session -> session.singleUseObjects().contains(claimKey), Boolean.class), is(true));

        tamperAuthenticatorLabelOnSubmit(labelValue);

        events.clear();
        webAuthnRegisterPage.clickRegister();
        if (webAuthnRegisterPage.isRegisterAlertPresent()) {
            // Accept the prompt with a dummy value; the submit hook overwrites or removes the label.
            webAuthnRegisterPage.registerWebAuthnCredential(SecretGenerator.getInstance().randomString(24));
        }

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("Please specify a label for your Passkey."));
        EventAssertion.assertError(events.poll())
                .error(Errors.INVALID_REGISTRATION)
                .details(WebAuthnConstants.REG_ERR_LABEL, Messages.WEBAUTHN_ERROR_MISSING_LABEL);
        assertThat("No WebAuthn credential should be stored after a blank label", getWebAuthnCredentialCount(userId), is(0L));
        // The error page starts a new ceremony on "Try again", so this is the direct proof
        // that the original challenge was not consumed by the rejected request.
        assertThat("Original registration challenge should still be unused",
                runOnServer.fetch(session -> session.singleUseObjects().contains(claimKey), Boolean.class), is(true));

        webAuthnErrorPage.clickTryAgain();
        webAuthnRegisterPage.assertCurrent();
        String newChallenge = webAuthnRegisterPage.getChallenge();
        assertThat("Retry should generate a new challenge", newChallenge, is(not(challenge)));

        webAuthnRegisterPage.clickRegister();
        if (webAuthnRegisterPage.isRegisterAlertPresent()) {
            webAuthnRegisterPage.registerWebAuthnCredential("valid-passkey");
        }

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        assertThat("Exactly one credential after successful retry", getWebAuthnCredentialCount(userId), is(1L));
        String storedLabel = managedRealm.admin().users().get(userId).credentials().stream()
                .filter(c -> getCredentialType().equals(c.getType()))
                .findFirst()
                .orElseThrow()
                .getUserLabel();
        assertThat(storedLabel, is("valid-passkey"));
    }

    /**
     * {@code null} removes the field entirely (missing parameter). Otherwise the hidden field
     * is overwritten after the prompt so empty and whitespace-only values actually reach the server.
     */
    private void tamperAuthenticatorLabelOnSubmit(String labelValue) {
        String script;
        if (labelValue == null) {
            script = "document.getElementById('register').addEventListener('submit', function() {" +
                    "  const field = document.getElementById('authenticatorLabel');" +
                    "  if (field) { field.remove(); }" +
                    "});";
        } else {
            String encoded = labelValue.replace("\\", "\\\\").replace("'", "\\'");
            script = "document.getElementById('register').addEventListener('submit', function() {" +
                    "  document.getElementById('authenticatorLabel').value = '" + encoded + "';" +
                    "});";
        }
        ((JavascriptExecutor) driver.driver()).executeScript(script);
    }

    private long getWebAuthnCredentialCount(String userId) {
        return managedRealm.admin().users().get(userId).credentials().stream()
                .filter(c -> getCredentialType().equals(c.getType()))
                .count();
    }
}
