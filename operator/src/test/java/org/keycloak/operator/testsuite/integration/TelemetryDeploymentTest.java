package org.keycloak.operator.testsuite.integration;

import java.util.List;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfApiServerTest
@QuarkusTest
public class TelemetryDeploymentTest extends BaseOperatorTest {

    @Test
    public void telemetryHeaders() {
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setImage(null); // doesn't seem to become ready with the custom image

        // telemetry
        K8sUtils.set(k8sclient, createSecret("telemetry-secret"));
        addHeaderWithSecret(kc, "telemetry-header-Authorization", "telemetry-secret");

        // telemetry logs
        K8sUtils.set(k8sclient, createSecret("telemetry-logs-secret"));
        addHeaderWithSecret(kc, "telemetry-logs-header-Authorization", "telemetry-logs-secret");

        // telemetry metrics
        K8sUtils.set(k8sclient, createSecret("telemetry-metrics-secret"));
        addHeaderWithSecret(kc, "telemetry-metrics-header-Authorization", "telemetry-metrics-secret");

        deployKeycloak(k8sclient, kc, true);

        var envVars = k8sclient
                .pods()
                .inNamespace(namespace)
                .withLabels(Constants.DEFAULT_LABELS)
                .list()
                .getItems()
                .get(0)
                .getSpec()
                .getContainers()
                .get(0)
                .getEnv();

        assertHeader(envVars, "KC_TELEMETRY_HEADER_AUTHORIZATION", "telemetry-secret");
        assertHeader(envVars, "KC_TELEMETRY_LOGS_HEADER_AUTHORIZATION", "telemetry-logs-secret");
        assertHeader(envVars, "KC_TELEMETRY_METRICS_HEADER_AUTHORIZATION", "telemetry-metrics-secret");
    }

    protected void assertHeader(List<EnvVar> envVars, String headerEnvVar, String secretName) {
        assertTrue(envVars.stream().anyMatch(
                e -> e.getName().equals(headerEnvVar) && e.getValueFrom() != null
                        && e.getValueFrom().getSecretKeyRef().getName().equals(secretName) && e.getValueFrom().getSecretKeyRef().getKey().equals("token")));

    }

    protected Secret createSecret(String secretName) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespace)
                .endMetadata()
                .addToStringData("token", "<not-empty>")
                .build();
    }

    protected void addHeaderWithSecret(Keycloak kc, String optionName, String secretName) {
        kc.getSpec().getAdditionalOptions().add(new ValueOrSecret(optionName,
                new SecretKeySelectorBuilder()
                        .withName(secretName)
                        .withKey("token")
                        .build()));
    }
}
