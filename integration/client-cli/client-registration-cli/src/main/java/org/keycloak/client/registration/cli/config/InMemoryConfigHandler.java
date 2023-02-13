package org.keycloak.client.registration.cli.config;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class InMemoryConfigHandler implements ConfigHandler {

    private ConfigData cached;

    @Override
    public void saveMergeConfig(ConfigUpdateOperation config) {
        config.update(cached);
    }

    @Override
    public ConfigData loadConfig() {
        return cached;
    }

    public void setConfigData(ConfigData data) {
        this.cached = data;
    }
}
