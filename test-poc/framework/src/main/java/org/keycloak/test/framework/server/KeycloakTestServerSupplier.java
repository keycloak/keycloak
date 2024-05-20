package org.keycloak.test.framework.server;

import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;
import org.keycloak.test.framework.server.smallrye_config.TestConfigSource;

public class KeycloakTestServerSupplier implements Supplier<KeycloakTestServer, KeycloakIntegrationTest> {

    @Override
    public Class<KeycloakTestServer> getValueType() {
        return KeycloakTestServer.class;
    }

    @Override
    public Class<KeycloakIntegrationTest> getAnnotationClass() {
        return KeycloakIntegrationTest.class;
    }

    @Override
    public InstanceWrapper<KeycloakTestServer, KeycloakIntegrationTest> getValue(Registry registry, KeycloakIntegrationTest annotation) {
        TestConfigSource configSource = SupplierHelpers.getInstance(annotation.config());
        KeycloakTestServerSmallryeConfig smallryeConfig = new KeycloakTestServerSmallryeConfig(configSource);

        KeycloakTestServer keycloakTestServer = KeycloakTestServerProducer.createKeycloakTestServerInstance(smallryeConfig.getServerType());

        keycloakTestServer.start(smallryeConfig);

        return new InstanceWrapper<>(this, annotation, keycloakTestServer);
    }

    @Override
    public LifeCycle getLifeCycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceWrapper<KeycloakTestServer, KeycloakIntegrationTest> a, InstanceWrapper<KeycloakTestServer, KeycloakIntegrationTest> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(KeycloakTestServer remoteKeycloakTestServer) {
        remoteKeycloakTestServer.stop();
    }

}
