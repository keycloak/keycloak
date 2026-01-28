package org.keycloak.vault;

import java.nio.file.Paths;
import java.util.Arrays;

import org.keycloak.common.util.Environment;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.vault.SecretContains.secretContains;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

/**
 * Tests for {@link FilesKeystoreVaultProvider}.
 *
 * @author Peter Zaoral
 */
public class KeystoreVaultProviderTest {

    @Before
    public void before() {
        // TODO: improve when the supported keystore types for FIPS will be unified across the codebase
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void shouldObtainSecret() {
        //given
        // keytool -importpass -storetype pkcs12 -alias test_alias -keystore myks -storepass keystorepassword
        VaultProvider provider = new FilesKeystoreVaultProvider(Paths.get(Scenario.EXISTING.getAbsolutePathAsString() + "/myks"), "keystorepassword", "PKCS12","test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));

        //when
        VaultRawSecret secret1 = provider.obtainSecret("alias");

        //then
        assertNotNull(secret1);
        assertNotNull(secret1.get().get());
        assertThat(secret1, secretContains("topsecret"));
    }

    @Test
    public void shouldObtainSecretFromDifferentKeystoreType() {
        //given
        VaultProvider provider = new FilesKeystoreVaultProvider(Paths.get(Scenario.EXISTING.getAbsolutePathAsString() + "/myks.jceks"), "keystorepassword", "JCEKS", "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));

        //when
        VaultRawSecret secret1 = provider.obtainSecret("alias");

        //then
        assertNotNull(secret1);
        assertNotNull(secret1.get().get());
        assertThat(secret1, secretContains("topsecret"));
    }

    @Test
    public void shouldFailBecauseOfTypeMismatch() {
        //given
        VaultProvider provider = new FilesKeystoreVaultProvider(Paths.get(Scenario.EXISTING.getAbsolutePathAsString() + "/myks"), "keystorepassword", "JCEKS", "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));

        //when
        assertThrows("java.io.IOException: Invalid keystore format", RuntimeException.class, () -> provider.obtainSecret("alias"));
    }
}