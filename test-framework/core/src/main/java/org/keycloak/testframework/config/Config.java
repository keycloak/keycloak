package org.keycloak.testframework.config;

import io.quarkus.runtime.configuration.CharsetConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.keycloak.testframework.injection.ValueTypeAlias;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static ConfigSource initTestEnvConfigSource() {
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        while (Files.isDirectory(currentPath)) {
            Path envTestPath = currentPath.resolve(".env-test");
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
