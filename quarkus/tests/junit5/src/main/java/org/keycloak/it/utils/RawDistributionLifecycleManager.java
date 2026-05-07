package org.keycloak.it.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RawDistributionLifecycleManager implements QuarkusTestResourceLifecycleManager {

    RawKeycloakDistribution dist;
    
    @Override
    public Map<String, String> start() {
        dist = new RawKeycloakDistribution(
                false,
                false,
                false,
                true,
                false,
                8080);
        return null;
    }

    @Override
    public void stop() {
        try {
            FileUtil.deleteDirectory(dist.getDistPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
