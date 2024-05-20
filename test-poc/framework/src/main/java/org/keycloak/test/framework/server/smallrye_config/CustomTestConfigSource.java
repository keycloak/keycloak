package org.keycloak.test.framework.server.smallrye_config;

import org.keycloak.config.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomTestConfigSource implements TestConfigSource {

    private static final Map<String, String> configuration = new HashMap<>();

    @Override
    public int getOrdinal() {
        return 375;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return DefaultTestConfigSource.class.getSimpleName();
    }

    protected static <T> void addTestOption(Option<T> option, String value, String prefix) {
        String optionWithPrefix = prefix + "." + option.getKey();
        configuration.put(optionWithPrefix, value);
    }

    protected static <T> void addTestOption(Option<T> option, String value) {
        addTestOption(option, value, TestOption.prefix());
    }
}
