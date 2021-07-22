package org.keycloak.vault;

import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link AWSSecretsMgrProviderFactory}.
 */
public class AWSSecretsMgrProviderFactoryTest {

    @Test
    public void shouldInitializeVaultCorrectly() {
        //given
        VaultConfig config = new VaultConfig("region");
        KeycloakSession session = new DefaultKeycloakSession(new DefaultKeycloakSessionFactory());
        AWSSecretsMgrProviderFactory factory = new AWSSecretsMgrProviderFactory() {
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
    public void shouldReturnNullWhenWithNullDirectory() {
        //given
        VaultConfig config = new VaultConfig(null);
        AWSSecretsMgrProviderFactory factory = new AWSSecretsMgrProviderFactory();

        //when
        factory.init(config);
        VaultProvider provider = factory.create(null);

        //then
        assertNull(provider);
    }

    /**
     * A whitebox implementation of the config. Please use only for testing {@link AWSSecretsMgrProviderFactory}.
     */
    private static class VaultConfig implements Config.Scope {

        private String defaultRegion;

        public VaultConfig(String defaultRegion) {
            this.defaultRegion = defaultRegion;
        }

        @Override
        public String get(String key) {
            return "defaultRegion".equals(key) ? defaultRegion : null;
        }

        @Override
        public String get(String key, String defaultValue) {
            return get(key) != null ? get(key) : defaultValue;
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
    }
}
