package org.keycloak.vault;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Set;

/**
 * Tests for {@link FilesPlainTextVaultProviderFactory}.
 *
 * @author Sebastian Łaskawiec
 */
public class PlainTextVaultProviderFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldInitializeVaultCorrectly() {
        //given
        VaultConfig config = new VaultConfig(Scenario.EXISTING.getAbsolutePathAsString());
        KeycloakSession session = new DefaultKeycloakSession(new DefaultKeycloakSessionFactory());
        FilesPlainTextVaultProviderFactory factory = new FilesPlainTextVaultProviderFactory() {
            @Override
            protected String getRealmName(KeycloakSession session) {
                return "test";
            }
        };

        //when
        factory.init(config);
        VaultProvider provider = factory.create(session);

        //then
        assertNotNull(provider);
    }

    @Test
    public void shouldThrowAnExceptionWhenUsingNonExistingDirectory() {
        //given
        VaultConfig config = new VaultConfig(Scenario.NON_EXISTING.getAbsolutePathAsString());
        FilesPlainTextVaultProviderFactory factory = new FilesPlainTextVaultProviderFactory();

        expectedException.expect(VaultNotFoundException.class);

        //when
        factory.init(config);

        //then - verified by the ExpectedException rule
    }

    @Test
    public void shouldReturnNullWhenWithNullDirectory() {
        //given
        VaultConfig config = new VaultConfig(null);
        FilesPlainTextVaultProviderFactory factory = new FilesPlainTextVaultProviderFactory();

        //when
        factory.init(config);
        VaultProvider provider = factory.create(null);

        //then
        assertNull(provider);
    }

    /**
     * A whitebox implementation of the config. Please use only for testing {@link FilesPlainTextVaultProviderFactory}.
     */
    private static class VaultConfig implements Config.Scope {

        private String vaultDirectory;

        public VaultConfig(String vaultDirectory) {
            this.vaultDirectory = vaultDirectory;
        }

        @Override
        public String get(String key) {
            return "dir".equals(key) ? vaultDirectory : null;
        }

        @Override
        public String get(String key, String defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String[] getArray(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Integer getInt(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Long getLong(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Boolean getBoolean(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Config.Scope scope(String... scope) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Set<String> getPropertyNames() {
            throw new UnsupportedOperationException("not implemented");
        }
    }

}