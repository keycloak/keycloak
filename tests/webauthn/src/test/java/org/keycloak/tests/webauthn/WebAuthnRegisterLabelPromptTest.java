package org.keycloak.tests.webauthn;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.webauthn.page.WebAuthnRegisterPage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Alert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Browser coverage for the label prompt loop in {@code webauthnRegister.js}: a blank or
 * whitespace-only answer re-opens the prompt, and cancelling the prompt falls back to the
 * default label. Each prompt is answered through the real browser dialog.
 */
@KeycloakIntegrationTest
public class WebAuthnRegisterLabelPromptTest extends AbstractWebAuthnVirtualTest {

    private static final String DEFAULT_LABEL = "Passkey (Default Label)";

    /** {@code null} in a prompt sequence means the prompt is cancelled. */
    static Stream<Arguments> blankAnswersThenValidLabel() {
        return Stream.of(
                Arguments.of("empty-then-whitespace", Arrays.asList("", "   ", "my-passkey")),
                Arguments.of("whitespace-then-empty", Arrays.asList("   ", "", "my-passkey"))
        );
    }

    static Stream<Arguments> blankAnswerThenCancel() {
        return Stream.of(
                Arguments.of("empty-then-cancel", Arrays.asList("", null)),
                Arguments.of("whitespace-then-cancel", Arrays.asList("   ", null))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("blankAnswersThenValidLabel")
    public void blankPromptAnswersArePromptedAgainUntilValidLabel(String caseName, List<String> answers) {
        String userId = registerAnsweringPrompts("prompt-" + caseName, answers);

        assertRegisteredWithLabel(userId, "my-passkey");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("blankAnswerThenCancel")
    public void cancellingPromptAfterBlankAnswerUsesDefaultLabel(String caseName, List<String> answers) {
        String userId = registerAnsweringPrompts("prompt-" + caseName, answers);

        assertRegisteredWithLabel(userId, DEFAULT_LABEL);
    }

    /**
     * Registers a new user and answers the label prompt with each value of {@code answers} in turn.
     * Every answer must be preceded by an open prompt, so a blank answer that did not re-prompt fails here.
     */
    private String registerAnsweringPrompts(String caseName, List<String> answers) {
        String username = "label-" + (isPasswordless() ? "pwdless-" : "") + caseName;

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", username + "@email", username, PASSWORD);

        webAuthnRegisterPage.assertCurrent();
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), username).getId();
        managedRealm.cleanup().add(r -> r.users().get(userId).remove());

        webAuthnRegisterPage.clickRegister();
        for (int i = 0; i < answers.size(); i++) {
            assertThat("Label prompt should be open before answer #" + (i + 1),
                    webAuthnRegisterPage.isRegisterAlertPresent(WebAuthnRegisterPage.ALERT_DEFAULT_TIMEOUT), is(true));
            Alert prompt = driver.driver().switchTo().alert();
            String answer = answers.get(i);
            if (answer == null) {
                prompt.dismiss();
            } else {
                prompt.sendKeys(answer);
                prompt.accept();
            }
        }
        assertThat("No further label prompt expected after the final answer",
                isAlertPresentWithin(Duration.ofSeconds(WebAuthnRegisterPage.ALERT_CHECK_TIMEOUT)), is(false));
        return userId;
    }

    private void assertRegisteredWithLabel(String userId, String expectedLabel) {
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess(), "Registration should complete and log the user in");
        List<String> labels = managedRealm.admin().users().get(userId).credentials().stream()
                .filter(c -> getCredentialType().equals(c.getType()))
                .map(CredentialRepresentation::getUserLabel)
                .toList();
        assertThat("Exactly one credential should be stored", labels.size(), is(1));
        assertThat(labels.get(0), is(expectedLabel));
    }

    private boolean isAlertPresentWithin(Duration timeout) {
        try {
            new WebDriverWait(driver.driver(), timeout).until(ExpectedConditions.alertIsPresent());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
