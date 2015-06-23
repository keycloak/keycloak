package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.keycloak.testsuite.arquillian.undertow.CustomUndertowContainer;

/**
 *
 * @author tkyjovsk
 */
public class CustomExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        
        builder
                .service(DeploymentScenarioGenerator.class, DeploymentTargetModifier.class)
                .service(ApplicationArchiveProcessor.class, DeploymentArchiveProcessor.class)
                .override(ResourceProvider.class, URLResourceProvider.class, URLFixer.class);

        builder
                .service(DeployableContainer.class, CustomUndertowContainer.class);
        
    }

}
