package org.keycloak.operator;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CRSerializationTest {

    @Test
    public void testDeserialization() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        assertEquals("my-hostname", keycloak.getSpec().getHostname());
        assertEquals("my-image", keycloak.getSpec().getImage());
        assertEquals("my-tls-secret", keycloak.getSpec().getTlsSecret());
        assertTrue(keycloak.getSpec().isDisableDefaultIngress());
    }

}
