package org.keycloak.testsuite.arquillian;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import static org.keycloak.testsuite.AbstractKeycloakTest.AUTH_SERVER_URL;
import static org.keycloak.testsuite.AbstractKeycloakTest.REALM_KEY;
import static org.keycloak.testsuite.AbstractKeycloakTest.loadJson;
import org.keycloak.testsuite.KeycloakContainersManager;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
public class DeploymentArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger log = Logger.getLogger(DeploymentArchiveProcessor.class.getName());

    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        modifyAdapterConfig(archive, testClass);
    }

    protected void modifyAdapterConfig(Archive<?> archive, TestClass testClass) {
        if (testClass.getJavaClass().isAssignableFrom(KeycloakContainersManager.class)) {
            try {
                KeycloakContainersManager kcm = (KeycloakContainersManager) testClass.getJavaClass().cast(KeycloakContainersManager.class);

                BaseAdapterConfig adapterDConfig = loadJson(archive.get(ADAPTER_CONFIG_PATH)
                        .getAsset().openStream(), BaseAdapterConfig.class);

                if (kcm.isRelative()) {
                    adapterDConfig.setAuthServerUrl("/auth");
//                ac.setRealmKey(null); // TODO verify if realm key is required for relative scneario
                } else {
                    adapterDConfig.setAuthServerUrl(AUTH_SERVER_URL);
                    adapterDConfig.setRealmKey(REALM_KEY);
                }

                archive.add(new StringAsset(JsonSerialization.writeValueAsPrettyString(adapterDConfig)),
                        ADAPTER_CONFIG_PATH);

            } catch (IOException ex) {
                log.log(Level.SEVERE, "Cannot serialize adapter config to JSON.", ex);
            }
        }
    }

}
