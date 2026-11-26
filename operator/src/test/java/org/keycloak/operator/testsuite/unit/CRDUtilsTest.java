package org.keycloak.operator.testsuite.unit;

import org.keycloak.operator.crds.v2beta1.CRDUtils;
import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;

import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CRDUtilsTest {

    @Test
    @SuppressWarnings("unchecked")
    void convertsJobStatusUsingClientMapper() {
        Context<Keycloak> context = Mockito.mock(Context.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(context.getClient().getKubernetesSerialization()).thenReturn(new KubernetesSerialization());
        var status = new JobStatusBuilder().withSucceeded(1).withFailed(0).build();

        var json = CRDUtils.toJsonNode(status, context);

        assertTrue(json.isObject());
        assertEquals(1, json.get("succeeded").asInt());
        assertEquals(0, json.get("failed").asInt());
    }
}
