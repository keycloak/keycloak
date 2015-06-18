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
import org.keycloak.testsuite.adapter.Relative;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
public class ArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger log = Logger.getLogger(ArchiveProcessor.class.getName());

    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        try {

            BaseAdapterConfig ac = loadJson(archive.get(ADAPTER_CONFIG_PATH)
                    .getAsset().openStream(), BaseAdapterConfig.class);

            if (testClass.isAnnotationPresent(Relative.class)) {
                ac.setAuthServerUrl("/auth");
//                ac.setRealmKey(null);
                // TODO verify if realm key is required for relative scneario
            } else {
                ac.setAuthServerUrl(AUTH_SERVER_URL);
                ac.setRealmKey(REALM_KEY);
            }

            archive.add(new StringAsset(JsonSerialization.writeValueAsPrettyString(ac)),
                    ADAPTER_CONFIG_PATH);

        } catch (IOException ex) {
            log.log(Level.SEVERE, "Could not read /WEB-INF/keycloak.json from archive.", ex);
        }
    }

}
