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

import java.util.Set;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import static org.keycloak.subsystem.extension.authserver.AuthServerUtil.getHandler;

/**
 * Base class for operations that create overlays for an auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public abstract class AbstractAddOverlayHandler implements OperationStepHandler {

    protected static final String UPLOADED_FILE_OP_NAME = "uploaded-file-name";

    protected static final SimpleAttributeDefinition BYTES_TO_UPLOAD
            = new SimpleAttributeDefinitionBuilder("bytes-to-upload", ModelType.BYTES, false)
            .setAllowExpression(false)
            .setAllowNull(false)
            .build();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        //System.out.println("*** execute operation ***");
        //System.out.println(scrub(operation));

        String uploadFileName = operation.get(UPLOADED_FILE_OP_NAME).asString();
        String overlayPath = getOverlayPath(uploadFileName);
        String overlayName = AuthServerUtil.getAuthServerName(operation) + "-keycloak-overlay";
        PathAddress overlayAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, overlayName));

        boolean isOverlayExists = isOverlayExists(context, overlayName, PathAddress.EMPTY_ADDRESS);
        if (!isOverlayExists) {
            addOverlay(context, overlayAddress);
            if (!isHostController(context)) {
                addDeploymentToOverlay(context, overlayAddress, AuthServerUtil.getDeploymentName(operation));
            }
        }

        if (isHostController(context)) {
            addOverlayToServerGroups(context, overlayAddress, operation, overlayName);
        }

        // There is no way to do an overwrite of content from here because it involves
        // removing the overlay service in the runtime phase.  You have to remove
        // the content in a seperate operation.
        if (isOverlayExists && isContentExists(context, overlayAddress, overlayPath)) {
            throw new OperationFailedException(pathExistsMessage(overlayAddress, overlayPath));
        }

        addContent(context, overlayAddress, operation.get(BYTES_TO_UPLOAD.getName()).asBytes(), overlayPath);

        context.restartRequired();
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    private boolean isHostController(OperationContext context) {
        return context.getProcessType() == ProcessType.HOST_CONTROLLER;
    }

    private String pathExistsMessage(PathAddress overlayAddress, String overlayPath) {
        PathAddress contentAddress = overlayAddress.append("content", overlayPath);
        String msg = "Can not update overlay. ";
        msg += "First remove the overlay with CLI using the following command with the content path in double quotes:  ";
        msg += contentAddress.toCLIStyleString() + ":remove";
        return msg;
    }

    private boolean isOverlayExists(OperationContext context, String overlayName, PathAddress address) {
        Resource resource = context.readResourceFromRoot(address);
        return resource.getChildrenNames("deployment-overlay").contains(overlayName);
    }

    private boolean isContentExists(OperationContext context, PathAddress overlayAddress, String overlayPath) {
        Resource resource = context.readResourceFromRoot(overlayAddress);
        return resource.getChildrenNames("content").contains(overlayPath);
    }

    private void addOverlay(OperationContext context, PathAddress overlayAddress) {
        ModelNode op = Util.createAddOperation(overlayAddress);
        doAddStep(context, overlayAddress, op);
    }

    private void addDeploymentToOverlay(OperationContext context, PathAddress overlayAddress, String deploymentName) {
        PathAddress deploymentAddress = overlayAddress.append("deployment", deploymentName);
        ModelNode op = Util.createAddOperation(deploymentAddress);
        doAddStep(context, deploymentAddress, op);
    }

    // only call this if context.getProcessType() == ProcessType.HOST_CONTROLLER
    private void addOverlayToServerGroups(OperationContext context, PathAddress overlayAddress, ModelNode operation, String overlayName) {
        String myProfile = findMyProfile(operation);
        for (String serverGroup : getServerGroupNames(context)) {
            PathAddress address = PathAddress.pathAddress("server-group", serverGroup);
            ModelNode serverGroupModel = context.readResourceFromRoot(address).getModel();
            if (serverGroupModel.get("profile").asString().equals(myProfile)) {
                PathAddress serverGroupOverlayAddress = address.append(overlayAddress);
                boolean isOverlayExists = isOverlayExists(context, overlayName, address);
                if (!isOverlayExists) {
                    addOverlay(context, serverGroupOverlayAddress);
                    addDeploymentToOverlay(context, serverGroupOverlayAddress, AuthServerUtil.getDeploymentName(operation));
                }
            }
        }
    }

    // only call this if context.getProcessType() == ProcessType.HOST_CONTROLLER
    private String findMyProfile(ModelNode operation) {
        PathAddress address = PathAddress.pathAddress(operation.get("address"));
        return address.getElement(0).getValue();
    }

    private Set<String> getServerGroupNames(OperationContext context) {
        return context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS).getChildrenNames("server-group");
    }

    private void addContent(OperationContext context, PathAddress overlayAddress, byte[] bytes, String overlayPath) throws OperationFailedException {
        PathAddress contentAddress = overlayAddress.append("content", overlayPath);
        ModelNode op = Util.createAddOperation(contentAddress);

        ModelNode content = new ModelNode();
        content.get("bytes").set(bytes);
        op.get("content").set(content);

        doAddStep(context, contentAddress, op);
    }

    private void doAddStep(OperationContext context, PathAddress address, ModelNode operation) {
        //System.out.println("**** Adding Add Step ****");
        //System.out.println(scrub(operation).toString());
        context.addStep(operation, getHandler(context, address, ADD), OperationContext.Stage.MODEL);
    }

    // used for debugging
    private ModelNode scrub(ModelNode op) {
        ModelNode scrubbed = op.clone();
        if (scrubbed.has("content")) {
            scrubbed.get("content").set("BYTES REMOVED FOR DISPLAY");
        }
        if (scrubbed.has("bytes-to-upload")) {
            scrubbed.get("bytes-to-upload").set("BYTES REMOVED FOR DISPLAY");
        }
        return scrubbed;
    }

    /**
     * Get the WAR path where the overlay will live.
     *
     * @param file The name of the file being uploaded.
     * @return The overlay path as a String.
     */
    abstract String getOverlayPath(String fileName);
}
