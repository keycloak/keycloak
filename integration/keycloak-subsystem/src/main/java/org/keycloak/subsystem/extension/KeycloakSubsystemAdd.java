/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.extension;


import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.keycloak.subsystem.extension.authserver.KeycloakServerDeploymentProcessor;

/**
 * The Keycloak subsystem add update handler.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
class KeycloakSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final KeycloakSubsystemAdd INSTANCE = new KeycloakSubsystemAdd();

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        resource.getModel().setEmptyObject();


    }

    @Override
    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(KeycloakExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, 0, chooseDependencyProcessor());
                processorTarget.addDeploymentProcessor(KeycloakExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE, // PHASE
                        Phase.POST_MODULE_VALIDATOR_FACTORY - 1, // PRIORITY
                        chooseConfigDeploymentProcessor());
                processorTarget.addDeploymentProcessor(KeycloakExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE, // PHASE
                        Phase.POST_MODULE_VALIDATOR_FACTORY - 1, // PRIORITY
                        new KeycloakServerDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    private DeploymentUnitProcessor chooseDependencyProcessor() {
        if (Environment.isWildFly()) {
            return new KeycloakDependencyProcessorWildFly();
        } else {
            return new KeycloakDependencyProcessorAS7();
        }
    }

    private DeploymentUnitProcessor chooseConfigDeploymentProcessor() {
        if (Environment.isWildFly()) {
            return new KeycloakAdapterConfigDeploymentProcessor();
        } else {
            return new KeycloakAdapterConfigDeploymentProcessorAS7();
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        super.performRuntime(context, operation, model, verificationHandler, newControllers);

        ServiceController<KeycloakAdapterConfigService> controller = context.getServiceTarget()
                .addService(KeycloakAdapterConfigService.SERVICE_NAME, KeycloakAdapterConfigService.INSTANCE)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        newControllers.add(controller);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
