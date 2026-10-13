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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.test.impl.client.deployment.AnnotationDeploymentScenarioGenerator;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.getAppServerQualifiers;

/**
 * Changes target container for all Arquillian deployments based on value of
 * @AppServerContainer.
 *
 * @author tkyjovsk
 */
public class DeploymentTargetModifier extends AnnotationDeploymentScenarioGenerator {

    // Will be replaced in runtime by real auth-server-container
    public static final String AUTH_SERVER_CURRENT = "auth-server-current";
    // Will be replaced in runtime by real app-server-container
    public static final String APP_SERVER_CURRENT = "app-server-current";

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    private Instance<TestContext> testContext;

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        TestContext context = testContext.get();
        if (context.isAdapterTest() && !context.isAdapterContainerEnabled() && !context.isAdapterContainerEnabledCluster()) {
            return new ArrayList<>(); // adapter test will be skipped, no need to genarate dependencies
        }

        List<DeploymentDescription> deployments = super.generate(testClass);

        checkTestDeployments(deployments, testClass, context.isAdapterTest());
        Set<String> appServerQualifiers = getAppServerQualifiers(testClass.getJavaClass());
        if (appServerQualifiers.isEmpty()) return deployments; // no adapter test

        String appServerQualifier = appServerQualifiers.stream()
                .filter(q -> q.contains(AppServerTestEnricher.CURRENT_APP_SERVER))
                .findAny()
                .orElse(null);

        if (appServerQualifier.contains(";")) return deployments;

        if (appServerQualifier != null && !appServerQualifier.isEmpty()) {
            for (DeploymentDescription deployment : deployments) {
                final boolean containerMatches = deployment.getTarget() != null && deployment.getTarget().getName().startsWith(appServerQualifier);

                if (deployment.getTarget() == null || Objects.equals(deployment.getTarget().getName(), "_DEFAULT_")) {
                    log.debug("Setting target container for " + deployment.getName() + ": " + appServerQualifier);
                    deployment.setTarget(new TargetDescription(appServerQualifier));
                } else if (! containerMatches && !deployment.getArchive().getName().equals("run-on-server-classes.war")) {// run-on-server deployment can have different target
                    throw new RuntimeException("Inconsistency found: target container for " + deployment.getName()
                      + " is set to " + deployment.getTarget().getName()
                      + " but the test class targets " + appServerQualifier);
                }
            }
        }
        return deployments;
    }

    private void checkTestDeployments(List<DeploymentDescription> descriptions, TestClass testClass, boolean isAdapterTest) {
        for (DeploymentDescription deployment : descriptions) {
            if (deployment.getTarget() != null) {
                String containerQualifier = deployment.getTarget().getName();
                if (AUTH_SERVER_CURRENT.equals(containerQualifier) || (!isAdapterTest && "_DEFAULT_".equals(containerQualifier))) {
                    String newAuthServerQualifier = AuthServerTestEnricher.AUTH_SERVER_CONTAINER;
                    updateServerQualifier(deployment, testClass, newAuthServerQualifier);
                } else if (containerQualifier.contains(APP_SERVER_CURRENT)) {
                    String suffix = containerQualifier.split(APP_SERVER_CURRENT)[1];
                    String newAppServerQualifier = ContainerConstants.APP_SERVER_PREFIX  + AppServerTestEnricher.CURRENT_APP_SERVER + "-" + suffix;
                    updateServerQualifier(deployment, testClass, newAppServerQualifier);
                } else {
                    String newServerQualifier = StringPropertyReplacer.replaceProperties(containerQualifier, SystemEnvProperties.UNFILTERED::getProperty);
                    if (!newServerQualifier.equals(containerQualifier)) {
                        updateServerQualifier(deployment, testClass, newServerQualifier);
                    }
                }


            }
        }
    }

    private void updateServerQualifier(DeploymentDescription deployment, TestClass testClass, String newServerQualifier) {
        log.infof("Setting target container for deployment %s.%s: %s", testClass.getName(), deployment.getName(), newServerQualifier);
        deployment.setTarget(new TargetDescription(newServerQualifier));
    }

}
