package org.keycloak.tests.webauthn;

import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialPublicKeyConverter;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.webdriver.BrowserType;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@KeycloakIntegrationTest(config = WebAuthnMLDSATest.ServerConfig.class)
public class WebAuthnMLDSATest extends AbstractWebAuthnVirtualTest {

    private static final int MINIMUM_CHROME_VERSION = 155;

    @Override
    public void setUpVirtualAuthenticator() {
        assumeBrowserSupportsMLDSA();
        super.setUpVirtualAuthenticator();
    }

    void assumeBrowserSupportsMLDSA() {
        assumeTrue(driver.getBrowserType() == BrowserType.CHROME,
                "ML-DSA virtual authenticator tests require Chrome 155 or later; found " + driver.getBrowserType());
        Capabilities capabilities = ((HasCapabilities) driver.driver()).getCapabilities();
        String version = capabilities.getBrowserVersion();
        String majorVersion = version.split("\\.", 2)[0];
        assumeTrue(majorVersion.matches("\\d+"), "Cannot determine Chrome major version from '" + version + "'");
        // Chrome's virtual authenticator supports ML-DSA starting with Chrome 155.
        // https://crrev.com/c/8277372
        assumeTrue(Integer.parseInt(majorVersion) >= MINIMUM_CHROME_VERSION,
                "ML-DSA virtual authenticator tests require Chrome 155 or later; found Chrome " + version);
    }

    @Override
    public void removeVirtualAuthenticator() {
        if (getVirtualAuthManager() != null) {
            super.removeVirtualAuthenticator();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "ML-DSA-44, -48",
            "ML-DSA-65, -49",
            "ML-DSA-87, -50"
    })
    public void registerAndAuthenticate(String algorithm, long coseAlgorithm) {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), policy -> policy
                .signatureAlgorithms(List.of(algorithm))
                .attestationConveyancePreference("none")
                .userVerificationRequirement("discouraged")));

        registerDefaultUser(algorithm);
        assertTrue(oAuthClient.parseLoginResponse().isSuccess());

        String userId = userResource().toRepresentation().getId();
        String credentialType = getCredentialType();
        runOnServer.run(session -> {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
            CredentialModel stored = user.credentialManager().getStoredCredentialsByTypeStream(credentialType)
                    .findFirst().orElseThrow();
            WebAuthnCredentialData data = WebAuthnCredentialModel.createFromCredentialModel(stored).getWebAuthnCredentialData();
            COSEKey restored = new CredentialPublicKeyConverter(new ObjectConverter())
                    .convertToEntityAttribute(data.getCredentialPublicKey());
            assertEquals(coseAlgorithm, restored.getAlgorithm().getValue());
            assertEquals(COSEKeyType.AKP, restored.getKeyType());
            assertEquals("none", data.getAttestationStatementFormat());
            assertEquals(1, data.getCounter());
        });

        logout();
        authenticate(true);
        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("Passkey signature verification failed."));
        assertStoredCounter(userId, credentialType, 1);

        events.clear();
        authenticate(false);
        assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll()).userId(userId);
        assertStoredCounter(userId, credentialType, 3);
    }

    private void authenticate(boolean corruptSignature) {
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();
        webAuthnLoginPage.assertCurrent();
        if (corruptSignature) {
            ((JavascriptExecutor) driver.driver()).executeScript("""
                    document.getElementById('webauth').addEventListener('submit', function() {
                        const signature = document.getElementById('signature');
                        signature.value = (signature.value[0] === 'A' ? 'B' : 'A') + signature.value.slice(1);
                    }, { once: true });
                    """);
        }
        webAuthnLoginPage.clickAuthenticate();
    }

    private void assertStoredCounter(String userId, String credentialType, long expectedCounter) {
        runOnServer.run(session -> {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
            CredentialModel stored = user.credentialManager().getStoredCredentialsByTypeStream(credentialType)
                    .findFirst().orElseThrow();
            assertEquals(expectedCounter, WebAuthnCredentialModel.createFromCredentialModel(stored).getWebAuthnCredentialData().getCounter());
        });
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.WEBAUTHN_ML_DSA);
        }
    }
}
