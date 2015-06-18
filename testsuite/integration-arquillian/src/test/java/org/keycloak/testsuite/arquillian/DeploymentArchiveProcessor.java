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
import static org.keycloak.testsuite.KeycloakContainersManager.isRelative;
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
        System.out.println("Modifying adapter config for " + archive.getName() + ", testClass: " + testClass.getJavaClass().getSimpleName());
        if (KeycloakContainersManager.class.isAssignableFrom(testClass.getJavaClass())) {
            try {
                BaseAdapterConfig adapterConfig = loadJson(archive.get(ADAPTER_CONFIG_PATH)
                        .getAsset().openStream(), BaseAdapterConfig.class);

                boolean relative = isRelative(testClass.getJavaClass());
                System.out.println(" - setting " + (relative ? "" : "non-") + "relative auth-server-url");
                if (relative) {
                    adapterConfig.setAuthServerUrl("/auth");
//                ac.setRealmKey(null); // TODO verify if realm key is required for relative scneario
                } else {
                    adapterConfig.setAuthServerUrl(AUTH_SERVER_URL);
                    adapterConfig.setRealmKey(REALM_KEY);
                }

                archive.add(new StringAsset(JsonSerialization.writeValueAsPrettyString(adapterConfig)),
                        ADAPTER_CONFIG_PATH);

            } catch (IOException ex) {
                log.log(Level.SEVERE, "Cannot serialize adapter config to JSON.", ex);
            }
        }
    }

}
