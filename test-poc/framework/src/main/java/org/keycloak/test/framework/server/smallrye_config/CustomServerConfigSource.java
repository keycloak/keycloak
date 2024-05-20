package org.keycloak.test.framework.server.smallrye_config;


import org.keycloak.config.FeatureOptions;

public class CustomServerConfigSource extends CustomTestConfigSource {

    static {
        addTestOption(FeatureOptions.FEATURES, "update-email");
        addTestOption(FeatureOptions.FEATURES, "update-email", TestOption.embeddedPrefix());
        addTestOption(FeatureOptions.FEATURES, "update-email", TestOption.remotePrefix());
    }
}
