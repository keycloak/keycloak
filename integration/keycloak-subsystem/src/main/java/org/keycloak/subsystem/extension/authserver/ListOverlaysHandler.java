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
import java.util.TreeSet;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation to list all of the provider jars, theme jars, and keycloak-server.json that
 * have been uploaded to the auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class ListOverlaysHandler implements OperationStepHandler {
    static final String LIST_OVERLAYS_OPERATION = "list-overlays";

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(LIST_OVERLAYS_OPERATION, AuthServerDefinition.rscDescriptionResolver)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .build();

    static final OperationStepHandler INSTANCE = new ListOverlaysHandler();

    private ListOverlaysHandler() {}

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = context.getResult();
        result.setEmptyList();

        String overlayName = AuthServerUtil.getOverlayName(operation);
        boolean isOverlayExists = AuthServerUtil.isOverlayExists(context, overlayName, PathAddress.EMPTY_ADDRESS);
        if (isOverlayExists) {
            Set<String> overlays = new TreeSet<String>(getOverlayNames(context, overlayName));
            for (final String key : overlays) {
                result.add(key);
            }
        }

        context.stepCompleted();
    }

    private Set<String> getOverlayNames(OperationContext context, String overlayName) {
        PathAddress overlayAddr = PathAddress.pathAddress(DEPLOYMENT_OVERLAY, overlayName);
        Resource resource = context.readResourceFromRoot(overlayAddr);
        return resource.getChildrenNames(CONTENT);
    }
}
