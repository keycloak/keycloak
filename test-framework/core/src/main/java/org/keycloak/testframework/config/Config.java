package org.keycloak.testframework.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.keycloak.testframework.injection.ValueTypeAlias;

import io.quarkus.runtime.configuration.CharsetConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

public class Config {

    private static final SmallRyeConfig config = initConfig();
    private static ValueTypeAlias valueTypeAlias = new ValueTypeAlias();

    public static String getSelectedSupplier(Class<?> valueType) {
        return config.getOptionalValue("kc.test." + valueTypeAlias.getAlias(valueType), String.class).orElse(null);
    }

    public static String getIncludedSuppliers(Class<?> valueType) {
        return config.getOptionalValue("kc.test." + valueTypeAlias.getAlias(valueType) + ".suppliers.included", String.class).orElse(null);
    }

    public static String getExcludedSuppliers(Class<?> valueType) {
        return config.getOptionalValue("kc.test." + valueTypeAlias.getAlias(valueType) + ".suppliers.excluded", String.class).orElse(null);
    }

    public static String getSupplierConfig(Class<?> valueType) {
        return config.getOptionalValue("kc.test." + valueTypeAlias.getAlias(valueType) + ".config", String.class).orElse(null);
    }

    public static <T> T getValueTypeConfig(Class<?> valueType, String name, String defaultValue, Class<T> type) {
        name = getValueTypeFQN(valueType, name);
        Optional<T> optionalValue = config.getOptionalValue(name, type);
        if (optionalValue.isPresent()) {
            return optionalValue.get();
        } else if (defaultValue != null && !defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
            return config.getConverter(type).orElseThrow(() -> new RuntimeException("Converter for " + type + " not found")).convert(defaultValue);
        } else {
            return null;
        }
    }

    public static String getValueTypeFQN(Class<?> valueType, String name) {
        return "kc.test." + valueTypeAlias.getAlias(valueType) + "." + name;
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

    public static String getAdminUsername() {
        return "admin";
    }

    public static String getAdminPassword() {
        return "admin";
    }

    public static SmallRyeConfig initConfig() {
        SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withConverters(new Converter[]{ new CharsetConverter(), new MemorySizeConverter(), new InetSocketAddressConverter() })
                .withInterceptors(new LogConfigInterceptor())
                .withSources(new SuiteConfigSource());

        ConfigSource testEnvConfigSource = initTestEnvConfigSource();
        if (testEnvConfigSource != null) {
            configBuilder.withSources(testEnvConfigSource);
        }

        ConfigSource testConfigSource = initTestConfigSource();
        if (testConfigSource != null) {
            configBuilder.withSources(testConfigSource);
        }

        return configBuilder.build();
    }

    public static void registerValueTypeAlias(ValueTypeAlias valueTypeAlias) {
        Config.valueTypeAlias = valueTypeAlias;
    }

    private static ConfigSource initTestEnvConfigSource() {
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        while (Files.isDirectory(currentPath)) {
            Path envTestPath = currentPath.resolve(".env.test");
            if (Files.isRegularFile(envTestPath)) {
                try {
                    return new EnvConfigSource(ConfigSourceUtil.urlToMap(envTestPath.toUri().toURL()), 350);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            currentPath = currentPath.getParent();
            if (!Files.isRegularFile(currentPath.resolve("pom.xml"))) {
                break;
            }
        }
        return null;
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
