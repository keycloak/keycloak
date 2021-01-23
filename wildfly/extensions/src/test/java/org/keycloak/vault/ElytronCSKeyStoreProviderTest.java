/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.vault;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.wildfly.security.credential.store.WildFlyElytronCredentialStoreProvider;

/**
 * Tests for the {@link ElytronCSKeyStoreProvider} and associated {@link ElytronCSKeyStoreProviderFactory}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronCSKeyStoreProviderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Tests the initialization of the {@link ElytronCSKeyStoreProviderFactory} using a valid configuration. As a result,
     * the Elytron credential store security provider should have been installed by the factory.
     *
     * @throws Exception if an exception occurs while running the test.
     */
    @Test
    public void testInitFactoryWithValidConfig() throws Exception {
        ElytronCSKeyStoreProviderFactory factory = null;
        try {
            ProviderConfig config = new ProviderConfig("src/test/resources/org/keycloak/vault/credential-store.p12",
                    "MASK-3u2HNQaMogJJ8VP7J6gRIl;12345678;321", "PKCS12");
            config.setKeyResolvers("KEY_ONLY, REALM_UNDERSCORE_KEY");
            factory = new ElytronCSKeyStoreProviderFactory();

            factory.init(config);
            // should initialize without errors and the elytron credential store provider is installed.
            Assert.assertNotNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        } finally {
            if (factory != null) {
                factory.close();
            }
            // elytron credential store provider should be removed on close.
            Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        }
    }

    /**
     * Tests the initialization of the {@link ElytronCSKeyStoreProviderFactory} using an empty config (this happens when
     * the factory is loaded via SPI but is not configured in keycloak). The factory must initialize without errors but
     * it won't be able to create providers.
     *
     * @throws Exception if an error occurs while runnig the test.
     */
    @Test
    public void testInitFactoryWithEmptyConfig() throws Exception {
        ProviderConfig config = new ProviderConfig(null, null, null);
        ElytronCSKeyStoreProviderFactory factory = new ElytronCSKeyStoreProviderFactory();
        factory.init(config);
        // should initialize without exceptions being thrown, but the elytron credential store provider isn't installed.
        Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
    }

    /**
     * Tests the initialization of the {@link ElytronCSKeyStoreProviderFactory} using a config that points to a credential
     * store location that does not exist. The initialization is expected to fail in this case.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testInitFactoryWithInvalidLocationThrowsException() throws Exception {
        ProviderConfig config = new ProviderConfig("src/test/resources/org/keycloak/vault/non-existing.p12", "secretpw1!", "JCEKS");
        ElytronCSKeyStoreProviderFactory factory = new ElytronCSKeyStoreProviderFactory();
        this.expectedException.expect(VaultNotFoundException.class);
        try {
            factory.init(config);
        } finally {
            // make sure the elytron credential store provider wasn't installed.
            Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        }
    }

    /**
     * Tests the initialization of the {@link ElytronCSKeyStoreProviderFactory} using a config that specifies only invalid
     * key resolvers. One of the resolvers configured in this test is the {@code FACTORY_PROVIDED}, which is a valid name,
     * but the {@link ElytronCSKeyStoreProviderFactory} doesn't implement the {@code getFactoryResolver} method and as such
     * is unable to provide a valid resolver.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testInitFactoryWithInvalidResolversThrowsException() throws Exception {
        ProviderConfig config = new ProviderConfig("src/test/resources/org/keycloak/vault/credential-store.p12", "secretpw1!", "PKCS12");
        config.setKeyResolvers("INVALID_RESOLVER, FACTORY_PROVIDED");
        ElytronCSKeyStoreProviderFactory factory = new ElytronCSKeyStoreProviderFactory();
        this.expectedException.expect(VaultConfigurationException.class);
        try {
            factory.init(config);
        } finally {
            // make sure the elytron credential store provider wasn't installed.
            Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        }
    }

    /**
     * Tests the creation of a provider using the {@link ElytronCSKeyStoreProviderFactory}. The test plays with the configuration
     * to check if the factory returns a proper {@link ElytronCSKeyStoreProvider} instance when fed with valid configuration and
     * if {@code null} is returned when a required configuration property is missing.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testCreateProvider() throws Exception {
        ElytronCSKeyStoreProviderFactory factory = null;
        try {
            // init the factory with valid config and check it can successfully create a provider instance.
            ProviderConfig config = new ProviderConfig("src/test/resources/org/keycloak/vault/credential-store.p12", "secretpw1!", "PKCS12");
            factory = new ElytronCSKeyStoreProviderFactory() {
                @Override
                protected String getRealmName(KeycloakSession session) {
                    return "master";
                }
            };
            factory.init(config);
            Assert.assertNotNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));

            VaultProvider provider = factory.create(null);
            Assert.assertNotNull(provider);

            // init the factory without a location and check that it returns a null provider on create.
            config.setLocation(null);
            factory.init(config);
            provider = factory.create(null);
            Assert.assertNull(provider);

            // init the factory without a password and check that it returns a null provider on create.
            config.setLocation("src/test/resources/org/keycloak/vault/credential-store.p12");
            config.setPassword(null);
            factory.init(config);
            provider = factory.create(null);
            Assert.assertNull(provider);

            // init the factory with an invalid keystore type and check that it returns a null provider on create.
            config.setPassword("secretpw1!");
            config.setKeyStoreType("INV_TYPE");
            factory.init(config);
            provider = factory.create(null);
            Assert.assertNull(provider);
        } finally {
            if (factory != null) {
                factory.close();
            }
            Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        }
    }

    /**
     * Tests the retrieval of secrets using the {@link ElytronCSKeyStoreProvider}. The test relies on the factory to obtain
     * an instance of the provider and then checks if the provider is capable of retrieving secrets using the configured
     * Elytron credential store.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testRetrieveSecretFromVault() throws Exception {
        ElytronCSKeyStoreProviderFactory factory = null;
        try {
            ProviderConfig config = new ProviderConfig("src/test/resources/org/keycloak/vault/credential-store.p12",
                    "MASK-3u2HNQaMogJJ8VP7J6gRIl;12345678;321", "PKCS12");
            config.setKeyResolvers("KEY_ONLY, REALM_UNDERSCORE_KEY");
            factory = new ElytronCSKeyStoreProviderFactory() {
                @Override
                protected String getRealmName(KeycloakSession session) {
                    return "master";
                }
            };

            factory.init(config);
            Assert.assertNotNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));

            VaultProvider provider = factory.create(null);
            Assert.assertNotNull(provider);

            // obtain a secret using a key that exists (matches the key provided by the REALM_UNDERSCORE_KEY resolver).
            VaultRawSecret secret = provider.obtainSecret("smtp_key");
            Assert.assertNotNull(secret);
            Assert.assertTrue(secret.get().isPresent());
            Assert.assertThat(secret, SecretContains.secretContains("secure_master_smtp_secret"));

            // try to retrieve a secret using a key that doesn't exist (neither of the key resolvers provides a key that exists in the vault).
            secret = provider.obtainSecret("another_key");
            Assert.assertNotNull(secret);
            Assert.assertFalse(secret.get().isPresent());
        } finally {
            if (factory != null) {
                factory.close();
            }
            Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        }
    }

    /**
     * Tests the retrieval of secrets using a custom {@link VaultProviderFactory} that extends the {@link ElytronCSKeyStoreProviderFactory}
     * and overrides the {@code getFactoryResolver} method to return a key resolver that combines the realm and key using the
     * {@code realm###key} format. We have an entry ({@code test###smtp_key}) in the test credential store that matches the format ,
     * so using a config that sets the {@code keyResolvers} property to {@code FACTORY_PROVIDED} should result in the proper
     * secret being retrieved from the vault.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testRetrieveSecretUsingCustomFactory() throws Exception {
        ElytronCSKeyStoreProviderFactory factory = null;
        try {
            ProviderConfig config = new ProviderConfig("src/test/resources/org/keycloak/vault/credential-store.p12",
                    "MASK-3u2HNQaMogJJ8VP7J6gRIl;12345678;321", "PKCS12");
            config.setKeyResolvers("FACTORY_PROVIDED");
            factory = new ElytronCSKeyStoreProviderFactory() {
                @Override
                protected String getRealmName(KeycloakSession session) {
                    return "test";
                }

                @Override
                protected VaultKeyResolver getFactoryResolver() {
                    return (realm, key) -> realm + "###" + key;
                }
            };

            factory.init(config);
            Assert.assertNotNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));

            VaultProvider provider = factory.create(null);
            Assert.assertNotNull(provider);

            // obtain a secret using a key that exists (matches the key provided by the FACTORY_PROVIDED resolver).
            VaultRawSecret secret = provider.obtainSecret("smtp_key");
            Assert.assertNotNull(secret);
            Assert.assertTrue(secret.get().isPresent());
            Assert.assertThat(secret, SecretContains.secretContains("custom_smtp_secret"));
        } finally {
            if (factory != null) {
                factory.close();
            }
            Assert.assertNull(Security.getProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName()));
        }
    }

    /**
     * Implementation of {@link Config.Scope} to be used for the tests.
     */
    private static class ProviderConfig implements Config.Scope {

        private Map<String, String> config = new HashMap<>();

        ProviderConfig(final String location, final String password, final String keyStoreType) {
            this.config.put(ElytronCSKeyStoreProviderFactory.CS_LOCATION, location);
            this.config.put(ElytronCSKeyStoreProviderFactory.CS_SECRET, password);
            this.config.put(ElytronCSKeyStoreProviderFactory.CS_KEYSTORE_TYPE, keyStoreType);
        }

        void setLocation(final String location) {
            this.config.put(ElytronCSKeyStoreProviderFactory.CS_LOCATION, location);
        }

        void setPassword(final String password) {
            this.config.put(ElytronCSKeyStoreProviderFactory.CS_SECRET, password);
        }

        void setKeyStoreType(final String keyStoreType) {
            this.config.put(ElytronCSKeyStoreProviderFactory.CS_KEYSTORE_TYPE, keyStoreType);
        }

        void setKeyResolvers(final String keyResolvers) {
            this.config.put(AbstractVaultProviderFactory.KEY_RESOLVERS, keyResolvers);
        }

        @Override
        public String get(String key) {
            return this.config.get(key);
        }

        @Override
        public String get(String key, String defaultValue) {
            return this.config.get(key) != null ? this.config.get(key) : defaultValue;
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

    static class SecretContains extends TypeSafeMatcher<VaultRawSecret> {

        private String thisVaultAsString;

        SecretContains(String thisVaultAsString) {
            this.thisVaultAsString = thisVaultAsString;
        }

        @Override
        protected boolean matchesSafely(VaultRawSecret secret) {
            String convertedSecret = StandardCharsets.UTF_8.decode(secret.get().get()).toString();
            return thisVaultAsString.equals(convertedSecret);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is equal to " + thisVaultAsString);
        }

        static Matcher<VaultRawSecret> secretContains(String thisVaultAsString) {
            return new SecretContains(thisVaultAsString);
        }
    }
}
