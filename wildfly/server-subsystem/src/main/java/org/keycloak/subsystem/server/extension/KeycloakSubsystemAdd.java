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
package org.keycloak.subsystem.server.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

import static org.keycloak.subsystem.server.extension.KeycloakExtension.SUBSYSTEM_NAME;
import static org.keycloak.subsystem.server.extension.KeycloakSubsystemDefinition.ALL_ATTRIBUTES;
import static org.keycloak.subsystem.server.extension.KeycloakSubsystemDefinition.WEB_CONTEXT;

/**
 * The Keycloak subsystem add update handler.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
class KeycloakSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final KeycloakSubsystemAdd INSTANCE = new KeycloakSubsystemAdd();

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.DEPENDENCIES, 0, new KeycloakProviderDependencyProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME,
                        Phase.POST_MODULE, // PHASE
                        Phase.POST_MODULE_VALIDATOR_FACTORY - 2, // PRIORITY
                        new KeycloakProviderDeploymentProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME,
                        Phase.POST_MODULE, // PHASE
                        Phase.POST_MODULE_VALIDATOR_FACTORY - 1, // PRIORITY
                        new KeycloakServerDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();
        
        // set attribute values from parsed model
        for (AttributeDefinition attrDef : ALL_ATTRIBUTES) {
            attrDef.validateAndSet(operation, model);
        }

        // returns early if on domain controller
        if (!requiresRuntime(context)) {
            return;
        }

        // don't want to try to start server on host controller
        if (!context.isNormalServer()) {
            return;
        }

        ModelNode webContextNode = resource.getModel().get(WEB_CONTEXT.getName());
        if (!webContextNode.isDefined()) {
            webContextNode = WEB_CONTEXT.getDefaultValue();
        }
        String webContext = webContextNode.asString();

        ServerUtil serverUtil = new ServerUtil(operation);
        serverUtil.addStepToUploadServerWar(context);
        KeycloakAdapterConfigService.INSTANCE.setWebContext(webContext);
        
        KeycloakAdapterConfigService.INSTANCE.updateConfig(operation, model);
    }
}
