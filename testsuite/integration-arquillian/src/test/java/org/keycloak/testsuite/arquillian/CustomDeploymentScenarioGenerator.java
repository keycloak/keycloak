package org.keycloak.testsuite.arquillian;

import java.util.List;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.test.impl.client.deployment.AnnotationDeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;

/**
 *
 * @author tkyjovsk
 */
public class CustomDeploymentScenarioGenerator extends AnnotationDeploymentScenarioGenerator {
    
    /**
     * Assigns target container to deployments based on value of
     * @TargetsCustomContainer annotation.
     *
     * @param testClass
     * @return
     */
    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        List<DeploymentDescription> deployments = super.generate(testClass);

        if (testClass.isAnnotationPresent(TargetsContainer.class)) {
            String targetContainer = ((TargetsContainer) testClass.getAnnotation(TargetsContainer.class)).value();
            if (targetContainer != null && !targetContainer.isEmpty()) {
                for (DeploymentDescription deployment : deployments) {
                    if (deployment.getTarget() == null || !deployment.getTarget().getName().equals(targetContainer)) {
                        deployment.setTarget(new TargetDescription(targetContainer));
                    }
                }
            }
        }

        return deployments;
    }

}
