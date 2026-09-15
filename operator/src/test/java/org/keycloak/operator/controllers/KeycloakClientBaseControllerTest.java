package org.keycloak.operator.controllers;

import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusCondition;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeycloakClientBaseControllerTest {

    @Test
    public void testErrorStatus() {
        var client = new KeycloakOIDCClientBuilder().withNewMetadata().endMetadata().build();
        var agg = new KeycloakClientBaseController.KeycloakClientStatusAggregator(client);
        agg.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, true, "some error");
        var status = agg.build();
        var condition = status.getConditions().get(0);
        assertEquals(KeycloakClientStatusCondition.HAS_ERRORS, condition.getType());
        assertEquals(true, condition.getStatus());
        assertEquals("some error", condition.getMessage());

        client.getMetadata().setGeneration(1L);
        client.setStatus(status);
        agg = new KeycloakClientBaseController.KeycloakClientStatusAggregator(client);
        agg.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, false, "");
    }

}
