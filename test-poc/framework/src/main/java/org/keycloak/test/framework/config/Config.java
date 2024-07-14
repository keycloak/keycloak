package org.keycloak.test.framework.config;

import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.keycloak.test.framework.injection.ValueTypeAlias;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private static final SmallRyeConfig config;

    private static final String kcTestConfigPath = System.getProperty("kc.test.config");

    static {
        List<ConfigSource> configSources = new ArrayList<>();

        if (kcTestConfigPath != null) {
            configSources.add(new FileConfigSource());
        }

        config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(configSources)
                .withSources(new DotEnvConfigSourceProvider())
                .build();
    }

    public static String getSelectedSupplier(Class valueType) {
        String key = "kc.test." + ValueTypeAlias.getAlias(valueType);

        if (config.isPropertyPresent(key)) {
            return config.getValue(key, String.class);
        }

        return null;
    }

}
