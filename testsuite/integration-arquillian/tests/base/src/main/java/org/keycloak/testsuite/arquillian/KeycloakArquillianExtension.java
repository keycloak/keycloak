package org.keycloak.testsuite.arquillian;

import org.keycloak.testsuite.arquillian.provider.URLProvider;
import org.keycloak.testsuite.arquillian.provider.SuiteContextProvider;
import org.keycloak.testsuite.arquillian.provider.TestContextProvider;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.graphene.location.CustomizableURLResourceProvider;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.keycloak.testsuite.arquillian.jira.JiraTestExecutionDecider;
import org.keycloak.testsuite.arquillian.provider.AdminClientProvider;
import org.keycloak.testsuite.arquillian.undertow.CustomUndertowContainer;

/**
 *
 * @author tkyjovsk
 */
public class KeycloakArquillianExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {

        builder
                .service(ResourceProvider.class, SuiteContextProvider.class)
                .service(ResourceProvider.class, TestContextProvider.class)
                .service(ResourceProvider.class, AdminClientProvider.class);

        builder
                .service(DeploymentScenarioGenerator.class, DeploymentTargetModifier.class)
                .service(ApplicationArchiveProcessor.class, DeploymentArchiveProcessor.class)
                .observer(ContainersTestEnricher.class);

        builder
                .service(DeployableContainer.class, CustomUndertowContainer.class);

        builder
                .service(TestExecutionDecider.class, JiraTestExecutionDecider.class);

        builder
                .override(ResourceProvider.class, URLResourceProvider.class, URLProvider.class)
                .override(ResourceProvider.class, CustomizableURLResourceProvider.class, URLProvider.class);

    }

}
