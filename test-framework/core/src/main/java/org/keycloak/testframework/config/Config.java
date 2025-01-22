package org.keycloak.testframework.config;

import io.quarkus.runtime.configuration.CharsetConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.keycloak.testframework.injection.ValueTypeAlias;

import java.io.File;
import java.net.URL;

public class Config {

    private static final SmallRyeConfig config = initConfig();

    public static String getSelectedSupplier(Class<?> valueType, ValueTypeAlias valueTypeAlias) {
        return config.getOptionalValue("kc.test." + valueTypeAlias.getAlias(valueType), String.class).orElse(null);
    }

    public static <T> T get(String name, T defaultValue, Class<T> clazz) {
        return config.getOptionalValue(name, clazz).orElse(defaultValue);
    }

    public static SmallRyeConfig getConfig() {
        return config;
    }

    public static String getAdminClientId() {
        return "temp-admin";
    }

    public static String getAdminClientSecret() {
        return "mysecret";
    }

    public static SmallRyeConfig initConfig() {
        SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withConverters(new Converter[]{ new CharsetConverter(), new MemorySizeConverter(), new InetSocketAddressConverter() })
                .withInterceptors(new LogConfigInterceptor());

        ConfigSource testConfigSource = initTestConfigSource();
        if (testConfigSource != null) {
            configBuilder.withSources(testConfigSource);
        }

        return configBuilder.build();
    }

    private static ConfigSource initTestConfigSource() {
        try {
            URL testConfig;
            String testConfigFile = System.getProperty("kc.test.config", System.getenv("KC_TEST_CONFIG"));
            if (testConfigFile != null) {
                testConfig = new File(testConfigFile).toURI().toURL();
            } else {
                testConfig = Thread.currentThread().getContextClassLoader().getResource("keycloak-test.properties");
            }
            return testConfig != null ? new PropertiesConfigSource(testConfig, 280) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
