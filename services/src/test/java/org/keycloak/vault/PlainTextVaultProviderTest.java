package org.keycloak.vault;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.vault.SecretContains.secretContains;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link FilesPlainTextVaultProvider}.
 *
 * @author Sebastian Łaskawiec
 */
public class PlainTextVaultProviderTest {

    private static final Logger logger = Logger.getLogger("org.keycloak.vault");
    private BlockingQueue<String> logMessages;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    private Handler logHandler;

    @Before
    public void setUp() {
        logMessages = new LinkedBlockingQueue<>();
        logger.setLevel(Level.WARNING);
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logMessages.add(record.getMessage());
            }

            @Override
            public void flush() { }

            @Override
            public void close() throws SecurityException { }
        };
        logger.addHandler(logHandler);
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void tearDown() {
        logger.removeHandler(logHandler);
        System.setErr(originalErr);
    }

    @Test
    public void shouldObtainSecret() throws Exception {
        //given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.EXISTING.getPath(), "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));

        //when
        VaultRawSecret secret1 = provider.obtainSecret("key1");

        //then
        assertNotNull(secret1);
        assertNotNull(secret1.get().get());
        assertThat(secret1, secretContains("secret1"));
    }

    @Test
    public void shouldReplaceUnderscoreWithTwoUnderscores() throws Exception {
        //given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.EXISTING.getPath(), "test_realm",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));
        //when
        VaultRawSecret secret1 = provider.obtainSecret("underscore_key1");

        //then
        assertNotNull(secret1);
        assertNotNull(secret1.get().get());
        assertThat(secret1, secretContains("underscore_secret1"));
    }

    @Test
    public void shouldReturnEmptyOptionalOnMissingSecret() throws Exception {
        //given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.EXISTING.getPath(), "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));


        //when
        VaultRawSecret secret = provider.obtainSecret("non-existing-key");

        //then
        assertNotNull(secret);
        assertFalse(secret.get().isPresent());
    }

    @Test
    public void shouldOperateOnNonExistingVaultDirectory() throws Exception {
        //given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.NON_EXISTING.getPath(), "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver()));

        //when
        VaultRawSecret secret = provider.obtainSecret("non-existing-key");

        //then
        assertNotNull(secret);
        assertFalse(secret.get().isPresent());
    }

    @Test
    public void shouldOperateOnRealmDirectory() throws Exception {
        //given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.EXISTING.getPath(), "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_FILESEPARATOR_KEY.getVaultKeyResolver()));

        //when
        VaultRawSecret secret = provider.obtainSecret("key2");

        //then
        assertNotNull(secret);
        assertNotNull(secret.get().get());
        assertThat(secret, secretContains("secret2"));
    }

    @Test
    public void shouldObtainSecretWithMultipleResolvers() throws Exception {
        //given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.EXISTING.getPath(), "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver(),
                        AbstractVaultProviderFactory.AvailableResolvers.REALM_FILESEPARATOR_KEY.getVaultKeyResolver()));

        //when (there's no test_key2 file matching the realm_underscore_key resolver, but we have a test/key2 file that matches the realm_fileseparator_key resolver)
        VaultRawSecret secret = provider.obtainSecret("key2");

        //then
        assertNotNull(secret);
        assertNotNull(secret.get().get());
        assertThat(secret, secretContains("secret2"));
    }

    @Test
    public void shouldReflectChangesInASecretFile() throws Exception {
        //given
        Path temporarySecretFile = Files.createTempFile("vault", null);
        Path vaultDirectory = temporarySecretFile.getParent();
        String secretName = temporarySecretFile.getFileName().toString();

        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(vaultDirectory, "ignored",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver()));

        //when
        String secret1AsString = null;
        String secret2AsString = null;

        Files.write(temporarySecretFile, "secret1".getBytes());
        try (VaultRawSecret secret1 = provider.obtainSecret(secretName)) {
            secret1AsString = StandardCharsets.UTF_8.decode(secret1.get().get()).toString();
        }

        Files.write(temporarySecretFile, "secret2".getBytes());
        try (VaultRawSecret secret2 = provider.obtainSecret(secretName)) {
            secret2AsString = StandardCharsets.UTF_8.decode(secret2.get().get()).toString();
        }

        //then
        assertEquals("secret1", secret1AsString);
        assertEquals("secret2", secret2AsString);
    }

    @Test
    public void shouldNotOverrideFileWhenDestroyingASecret() throws Exception {
        //given
        Path temporarySecretFile = Files.createTempFile("vault", null);
        Path vaultDirectory = temporarySecretFile.getParent();
        String secretName = temporarySecretFile.getFileName().toString();

        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(vaultDirectory, "ignored",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver()));

        Files.write(temporarySecretFile, "secret".getBytes());

        //when
        VaultRawSecret secretAfterFirstRead = provider.obtainSecret(secretName);
        assertThat(secretAfterFirstRead, secretContains("secret"));
        secretAfterFirstRead.close();
        VaultRawSecret secretAfterSecondRead = provider.obtainSecret(secretName);

        //then
        assertThat(secretAfterFirstRead, not(secretContains("secret")));
        assertThat(secretAfterSecondRead, secretContains("secret"));
    }

    @Test
    public void shouldPreventPathFileSeparatorInVaultSecretId() {
        // given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(
                Scenario.EXISTING.getPath(),
                "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_FILESEPARATOR_KEY.getVaultKeyResolver())
        );

        // when
        VaultRawSecret secret = provider.obtainSecret(".../key1");

        // then
        assertNotNull(secret);
        assertFalse(secret.get().isPresent());
        // The validation may be performed by AbstractVaultProvider or FilesPlainTextVaultProvider
        assertTrue(
            logMessages.stream().anyMatch(msg ->
                msg.contains("Key .../key1 contains invalid file separator character")
                    || msg.contains("Path traversal attempt detected in secret .../key1")
            )
        );
    }

    @Test
    public void shouldNotValidateWithInvalidPath() {
        // given
        Path vaultPath = Paths.get("/vault");
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(vaultPath, "test_realm",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.REALM_FILESEPARATOR_KEY.getVaultKeyResolver()));
        VaultKeyResolver resolver = AbstractVaultProviderFactory.AvailableResolvers.REALM_FILESEPARATOR_KEY.getVaultKeyResolver();
        String key = "key1";
        String resolvedKey = "../key1";

        // when
        boolean isValid = provider.validate(resolver, key, resolvedKey);

        // then
        assertFalse(isValid);
    }

    @Test
    public void shouldValidateWithDifferentResolver() {
        // given
        Path vaultPath = Paths.get("/vault");
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(vaultPath, "test_realm",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver()));
        VaultKeyResolver resolver = AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver();
        String key = "key1";
        String resolvedKey = "key1";

        // when
        boolean isValid = provider.validate(resolver, key, resolvedKey);

        // then
        assertTrue(isValid);
    }

    @Test
    public void shouldSearchForEscapedKeyOnlySecret() throws Exception {
        // given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(Scenario.EXISTING.getPath(), "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver()));

        // when
        VaultRawSecret secret = provider.obtainSecret("keyonly_escaped");

        // then
        assertNotNull(secret);
        assertNotNull(secret.get().get());
        assertThat(secret, secretContains("expected_secret_value"));
    }

    @Test
    public void shouldSearchForKeyOnlyLegacy() throws Exception {
        // given
        FilesPlainTextVaultProvider provider = new FilesPlainTextVaultProvider(
                Scenario.EXISTING.getPath(),
                "test",
                Arrays.asList(AbstractVaultProviderFactory.AvailableResolvers.KEY_ONLY.getVaultKeyResolver())
        );

        // when
        VaultRawSecret secret = provider.obtainSecret("keyonly_legacy");

        // then
        assertNotNull(secret);
        assertFalse(secret.get().isPresent());
        assertTrue(
                logMessages.stream()
                        .anyMatch(msg -> msg.contains("Secret was found using legacy key 'keyonly_legacy'. Please rename the key to 'keyonly__legacy' and repeat the action."))
        );
    }

}
