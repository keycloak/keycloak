package org.keycloak.tests.admin.finegrainedadminv1;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = AbstractFineGrainedAdminTest.FineGrainedAdminServerConf.class)
public class FineGrainedAdminWithTokenExchangeDisabledTest extends AbstractFineGrainedAdminTest{

    @Test
    public void testTokenExchangeDisabled() {
        checkTokenExchange(false);
    }
}
