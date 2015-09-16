package org.keycloak.testsuite.arquillian;

import java.util.List;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.test.impl.client.deployment.AnnotationDeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import static org.keycloak.testsuite.arquillian.ContainersTestEnricher.*;

/**
 * Changes target container for all Arquillian deployments based on value of
 * @AppServerContainer.
 *
 * @author tkyjovsk
 */
public class DeploymentTargetModifier extends AnnotationDeploymentScenarioGenerator {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        List<DeploymentDescription> deployments = super.generate(testClass);

        String appServerQualifier = getAppServerQualifier(
                testClass.getJavaClass());

        if (appServerQualifier != null && !appServerQualifier.isEmpty()) {
            for (DeploymentDescription deployment : deployments) {
                if (deployment.getTarget() == null || !deployment.getTarget().getName().equals(appServerQualifier)) {
                    log.debug("Setting target container for " + deployment.getName() + ": " + appServerQualifier);
                    deployment.setTarget(new TargetDescription(appServerQualifier));
                }
            }
        }

        return deployments;
    }

}
