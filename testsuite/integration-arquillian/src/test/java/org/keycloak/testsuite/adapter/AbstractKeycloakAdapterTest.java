package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.AbstractKeycloakTest;
import java.net.URL;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;

public abstract class AbstractKeycloakAdapterTest extends AbstractKeycloakTest {

    public static final String KEYCLOAK_ADAPTER_SERVER = "keycloak-adapter-managed";

    @Before
    public void startKeycloakAdapterServer() {
        controller.start(KEYCLOAK_ADAPTER_SERVER);
    }


}
