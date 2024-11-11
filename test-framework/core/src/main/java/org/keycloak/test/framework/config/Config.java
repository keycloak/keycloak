package org.keycloak.test.framework.config;

import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.keycloak.test.framework.injection.ValueTypeAlias;

import java.io.File;

public class Config {

    private static final SmallRyeConfig config = initConfig();

    public static String getSelectedSupplier(Class<?> valueType, ValueTypeAlias valueTypeAlias) {
        return config.getOptionalValue("kc.test." + valueTypeAlias.getAlias(valueType), String.class).orElse(null);
    }

    public static <T> T get(String name, T defaultValue, Class<T> clazz) {
        return config.getOptionalValue(name, clazz).orElse(defaultValue);
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
                .withSources(new DotEnvConfigSourceProvider());

        ConfigSource testConfigSource = initTestConfigSource();
        if (testConfigSource != null) {
            configBuilder.withSources(testConfigSource);
        }

        return configBuilder.build();
    }

    private static ConfigSource initTestConfigSource() {
        try {
            String testConfigFile = System.getProperty("kc.test.config", System.getenv("KC_TEST_CONFIG"));
            return testConfigFile != null ? new PropertiesConfigSource(new File(testConfigFile).toURI().toURL()) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
