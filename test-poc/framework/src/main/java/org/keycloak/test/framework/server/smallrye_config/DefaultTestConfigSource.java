package org.keycloak.test.framework.server.smallrye_config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultTestConfigSource implements TestConfigSource {

    private static final Map<String, String> configuration = new HashMap<>();

    static {
        addTestOption(TestOption.server(), "embedded");
        addTestOption(TestOption.embedded(), "embedded");
        addTestOption(TestOption.remote(), "remote");

        /*addTestOption(TestOption.features(), "");
        addTestOption(TestOption.embeddedFeatures(), "");
        addTestOption(TestOption.remoteFeatures(), "");

        configuration.put("keycloak.options.opt1", "none");
        configuration.put("%embedded.keycloak.options.opt1", "none");
        configuration.put("%remote.keycloak.options.opt1", "none");*/
    }

    @Override
    public int getOrdinal() {
        return 275;
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

    private static void addTestOption(String key, String value) {
        configuration.put(key, value);
    }
}
