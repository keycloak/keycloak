package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 *
 * @author tkyjovsk
 */
public class CustomExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeploymentScenarioGenerator.class, CustomDeploymentScenarioGenerator.class)
                .service(ApplicationArchiveProcessor.class, ArchiveProcessor.class);
    }

}
