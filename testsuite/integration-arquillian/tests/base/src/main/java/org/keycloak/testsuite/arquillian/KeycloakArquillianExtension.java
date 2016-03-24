/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.arquillian;

import org.keycloak.testsuite.arquillian.provider.*;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.graphene.location.ContainerCustomizableURLResourceProvider;
import org.jboss.arquillian.graphene.location.CustomizableURLResourceProvider;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.keycloak.testsuite.arquillian.h2.H2TestEnricher;
import org.keycloak.testsuite.arquillian.migration.MigrationTestExecutionDecider;

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
                .service(ResourceProvider.class, AdminClientProvider.class)
                .service(ResourceProvider.class, OAuthClientProvider.class);

        builder
                .service(DeploymentScenarioGenerator.class, DeploymentTargetModifier.class)
                .service(ApplicationArchiveProcessor.class, DeploymentArchiveProcessor.class)
                .observer(AuthServerTestEnricher.class)
                .observer(AppServerTestEnricher.class)
                .observer(H2TestEnricher.class);

        builder
                .service(TestExecutionDecider.class, MigrationTestExecutionDecider.class);

        builder
                .override(ResourceProvider.class, URLResourceProvider.class, URLProvider.class)
                .override(ResourceProvider.class, CustomizableURLResourceProvider.class, URLProvider.class)
                .override(ResourceProvider.class, ContainerCustomizableURLResourceProvider.class, URLProvider.class);

    }

}
