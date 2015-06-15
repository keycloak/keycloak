package org.keycloak.testsuite.adapter;

import org.junit.Before;
import org.keycloak.testsuite.AbstractKeycloakTest;

public abstract class AbstractKeycloakAdapterTest extends AbstractKeycloakTest {

    public static final String KEYCLOAK_ADAPTER_SERVER = "keycloak-adapter-managed";
    
    @Before
    public void startKeycloakAdapterServer() {
        controller.start(KEYCLOAK_ADAPTER_SERVER);
    }

}
