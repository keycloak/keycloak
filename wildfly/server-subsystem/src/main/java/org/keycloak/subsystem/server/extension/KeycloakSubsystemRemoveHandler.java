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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * Remove an auth-server from a realm.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public final class KeycloakSubsystemRemoveHandler extends ReloadRequiredRemoveStepHandler {

    static KeycloakSubsystemRemoveHandler INSTANCE = new KeycloakSubsystemRemoveHandler();

    private KeycloakSubsystemRemoveHandler() {}

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String deploymentName = ServerUtil.getDeploymentName(operation);
        KeycloakAdapterConfigService.INSTANCE.setWebContext(null);

        if (requiresRuntime(context)) { // don't do this on a domain controller
            addStepToRemoveServerWar(context, deploymentName);
        }

        super.performRemove(context, operation, model);
    }

    private void addStepToRemoveServerWar(OperationContext context, String deploymentName) {
        PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, deploymentName));
        ModelNode op = Util.createOperation(REMOVE, deploymentAddress);
        context.addStep(op, getRemoveHandler(context, deploymentAddress), OperationContext.Stage.MODEL);
    }

    private OperationStepHandler getRemoveHandler(OperationContext context, PathAddress address) {
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        return rootResourceRegistration.getOperationHandler(address, REMOVE);
    }
}
