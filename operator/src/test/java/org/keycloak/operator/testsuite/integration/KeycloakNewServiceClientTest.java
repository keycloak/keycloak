package org.keycloak.operator.testsuite.integration;

import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.crds.v2beta1.deployment.ValueOrSecret;
import org.keycloak.utils.StringUtil;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;

/**
 * Just temporary - once we use the new client service by default, we can remove this and the {@link KeycloakLegacyServiceClientTest}
 */
@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class KeycloakNewServiceClientTest extends KeycloakClientTest {

    @Override
    protected Keycloak getTestDeployment(boolean disableProbes) {
        Keycloak kc = super.getTestDeployment(disableProbes);

        String newClientServiceProperty = "-Dkc.admin-v2.client-service.legacy.enabled=false";
        var existingJavaOptsAppend = kc.getSpec().getEnv().stream()
                .filter(env -> "JAVA_OPTS_APPEND".equals(env.getName()))
                .findFirst()
                .orElse(null);

        if (existingJavaOptsAppend != null) {
            String existingValue = existingJavaOptsAppend.getValue();
            if (StringUtil.isNotBlank(existingValue)) {
                existingJavaOptsAppend.setValue(existingValue + " " + newClientServiceProperty);
            } else {
                existingJavaOptsAppend.setValue(newClientServiceProperty);
            }
        } else {
            kc.getSpec().getEnv().add(new ValueOrSecret("JAVA_OPTS_APPEND", newClientServiceProperty));
        }

        return kc;
    }
}
