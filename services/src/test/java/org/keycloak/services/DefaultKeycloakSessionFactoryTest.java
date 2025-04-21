package org.keycloak.services;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import java.util.HashMap;
import java.util.Map;

public class DefaultKeycloakSessionFactoryTest {

    private DummyConfigurationProvider config;
    private DummySpi spi;

    @Before
    public void before() {
        config = new DummyConfigurationProvider();
        Config.init(config);
    }

    @After
    public void after() {
        Config.init(new Config.SystemPropertiesConfigProvider());
    }

    @Test
    public void defaultProviderFromConfigTest() {
        Map<String, ProviderFactory> map = new HashMap<>(Map.of(
                "two", new DummyProviderFactory("two", 2),
                "one", new DummyProviderFactory("one", 0),
                "three", new DummyProviderFactory("three", 3)));
        spi = new DummySpi();

        // Default provider configured
        config.defaultProvider = "one";
        Assert.assertEquals("one", DefaultKeycloakSessionFactory.resolveDefaultProvider(map, spi));

        // Highest priority selected
        config.defaultProvider = null;
        Assert.assertEquals("three", DefaultKeycloakSessionFactory.resolveDefaultProvider(map, spi));

        // No default, with order=0
        map.values().stream().forEach(p -> ((DummyProviderFactory) p).order = 0);
        Assert.assertNull(DefaultKeycloakSessionFactory.resolveDefaultProvider(map, spi));

        // Provider with id=default selected
        map.put("default", new DummyProviderFactory("default", 0));
        Assert.assertEquals("default", DefaultKeycloakSessionFactory.resolveDefaultProvider(map, spi));

        // Default set if single provider exists
        map.remove("default");
        map.remove("two");
        map.remove("three");
        Assert.assertEquals("one", DefaultKeycloakSessionFactory.resolveDefaultProvider(map, spi));

        // Throw error if default configured not found
        config.defaultProvider = "nosuch";
        try {
            DefaultKeycloakSessionFactory.resolveDefaultProvider(map, spi);
            Assert.fail("Expected exception");
        } catch (RuntimeException e) {
            Assert.assertEquals("Failed to find provider nosuch for dummy", e.getMessage());
        }
    }

    private class DummyConfigurationProvider implements Config.ConfigProvider {

        String defaultProvider;

        @Override
        public String getProvider(String spi) {
            return null;
        }

        @Override
        public String getDefaultProvider(String spi) {
            return defaultProvider;
        }

        @Override
        public Config.Scope scope(String... scope) {
            return null;
        }
    }

    private class DummyProviderFactory implements ProviderFactory {

        private String id;
        private int order;

        public DummyProviderFactory(String id, int order) {
            this.id = id;
            this.order = order;
        }

        @Override
        public Provider create(KeycloakSession session) {
            return null;
        }

        @Override
        public void init(Config.Scope config) {
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
        }

        @Override
        public void close() {
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public int order() {
            return order;
        }
    }

    private class DummySpi implements Spi {

        @Override
        public boolean isInternal() {
            return false;
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public Class<? extends Provider> getProviderClass() {
            return null;
        }

        @Override
        public Class<? extends ProviderFactory> getProviderFactoryClass() {
            return null;
        }
    }

}
