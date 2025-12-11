package org.keycloak.testframework.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class SuiteConfigSource implements ConfigSource {

    private static final Map<String, String> SUITE_CONFIG = new HashMap<>();

    public static void set(String key, String value) {
        SUITE_CONFIG.put(key, value);
    }

    public static void clear() {
        SUITE_CONFIG.clear();
    }

    @Override
    public Set<String> getPropertyNames() {
        return SUITE_CONFIG.keySet();
    }

    @Override
    public String getValue(String s) {
        return SUITE_CONFIG.get(s);
    }

    @Override
    public String getName() {
        return "SuiteConfigSource";
    }

    @Override
    public int getOrdinal() {
        return 270;
    }
}
