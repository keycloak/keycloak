/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.subsystem.extension.authserver;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation to remove a provider jars, theme jars, or keycloak-server.json that
 * has been uploaded to the auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class RemoveOverlayHandler implements OperationStepHandler {
    static final String REMOVE_OVERLAY_OPERATION = "remove-overlay";

    protected static final SimpleAttributeDefinition OVERLAY_FILE_PATH =
            new SimpleAttributeDefinitionBuilder("overlay-file-path", ModelType.STRING, false)
            .setAllowExpression(true)
            .setAllowNull(false)
            .setDefaultValue(new ModelNode().set("/WEB-INF/lib/myprovider.jar"))
            .build();

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(REMOVE_OVERLAY_OPERATION, AuthServerDefinition.rscDescriptionResolver)
            .addParameter(OVERLAY_FILE_PATH)
            .addParameter(AbstractAddOverlayHandler.REDEPLOY_SERVER)
            .build();

    static final OperationStepHandler INSTANCE = new RemoveOverlayHandler();

    private RemoveOverlayHandler() {}

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String overlayName = AuthServerUtil.getOverlayName(operation);
        boolean isOverlayExists = AuthServerUtil.isOverlayExists(context, overlayName, PathAddress.EMPTY_ADDRESS);
        String overlayPath = operation.get(OVERLAY_FILE_PATH.getName()).asString();
        if (isOverlayExists) {
            PathAddress overlayAddress = AuthServerUtil.getOverlayAddress(overlayName);
            AbstractAddOverlayHandler.removeContent(context, overlayAddress, overlayPath);
        } else {
            context.setRollbackOnly();
            throw new OperationFailedException("Overlay path " + overlayPath + " not found.");
        }

        boolean isRedeploy = AbstractAddOverlayHandler.isRedeploy(context, operation);
        String deploymentName = AuthServerUtil.getDeploymentName(operation);
        if (isRedeploy) AuthServerUtil.addStepToRedeployAuthServer(context, deploymentName);
        if (!isRedeploy) context.restartRequired();

        context.stepCompleted();
    }
}
