package org.keycloak.tests.admin.finegrainedadminv1;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest(config = AbstractFineGrainedAdminTest.FineGrainedAdminServerConf.class)
public class FineGrainedAdminWithTokenExchangeDisabledTest extends AbstractFineGrainedAdminTest{

    @Test
    public void testTokenExchangeDisabled() {
        checkTokenExchange(false);
    }
}
