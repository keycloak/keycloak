/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian.containers;

import java.util.List;
import org.jboss.arquillian.container.impl.client.container.ContainerDeployController;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.event.DeployDeployment;
import org.jboss.arquillian.container.spi.event.DeployManagedDeployments;
import org.jboss.arquillian.container.spi.event.DeploymentEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

/**
 * Overrides a condition that container cannot be in manual mode, and deploys the deployment
 * if the container is started
 */
public class KeycloakContainerDeployController extends ContainerDeployController {
    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    private Instance<DeploymentScenario> deploymentScenario;

    @Inject
    private Instance<Injector> injector;

    @Override
    public void deployManaged(DeployManagedDeployments event) throws Exception {
        forEachManagedDeployment(new ContainerDeployController.Operation<Container, Deployment>() {
            @Inject
            private Event<DeploymentEvent> event;

            @Override
            public void perform(Container container, Deployment deployment) throws Exception {
                if (runOnServerDeploymentOnRemote(deployment)) return;
                if (container.getState().equals(Container.State.STARTED)) {
                    event.fire(new DeployDeployment(container, deployment));
                }
            }
        });
    }

    private void forEachManagedDeployment(ContainerDeployController.Operation<Container, Deployment> operation) throws Exception {
        DeploymentScenario scenario = this.deploymentScenario.get();
        if (scenario == null) {
            return;
        }
        forEachDeployment(scenario.managedDeploymentsInDeployOrder(), operation);
    }

    private void forEachDeployment(List<Deployment> deployments, ContainerDeployController.Operation<Container, Deployment> operation)
        throws Exception {
        injector.get().inject(operation);
        ContainerRegistry containerRegistry = this.containerRegistry.get();
        if (containerRegistry == null) {
            return;
        }
        for (Deployment deployment : deployments) {
            Container container = containerRegistry.getContainer(deployment.getDescription().getTarget());
            operation.perform(container, deployment);
        }
    }

    //do not deploy run-on-server-classes.war into server in remote mode
    private boolean runOnServerDeploymentOnRemote(Deployment deployment) {
        return AuthServerTestEnricher.isAuthServerRemote() && 
                deployment.getDescription().getArchive().getName().equals("run-on-server-classes.war");
    }
}
