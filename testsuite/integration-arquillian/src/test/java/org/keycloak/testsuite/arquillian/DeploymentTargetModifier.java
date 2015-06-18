package org.keycloak.testsuite.arquillian;

import java.util.List;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.test.impl.client.deployment.AnnotationDeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import static org.keycloak.testsuite.KeycloakContainersManager.*;

/**
 *
 * @author tkyjovsk
 */
public class DeploymentTargetModifier extends AnnotationDeploymentScenarioGenerator {

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        List<DeploymentDescription> deployments = super.generate(testClass);

        String keycloakAdapterServerQualifier = getAppServerQualifier(
                testClass.getJavaClass());

        if (keycloakAdapterServerQualifier != null && !keycloakAdapterServerQualifier.isEmpty()) {
            for (DeploymentDescription deployment : deployments) {
                if (deployment.getTarget() == null || !deployment.getTarget().getName().equals(keycloakAdapterServerQualifier)) {
                    deployment.setTarget(new TargetDescription(keycloakAdapterServerQualifier));
                }
            }
        }

        return deployments;
    }

}
