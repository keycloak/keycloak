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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import java.util.Set;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelType;
import org.keycloak.subsystem.extension.KeycloakAdapterConfigService;

/**
 * Rename the extension of an overlay in the overlays/<auth server> directory.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public final class ManageOverlayHandler extends AbstractRuntimeOnlyHandler {

    public static final String OP = "change-to";

    public static ManageOverlayHandler INSTANCE = new ManageOverlayHandler();

    public enum changeToEnum {deployed, undeployed};

    protected static final SimpleAttributeDefinition URL =
            new SimpleAttributeDefinitionBuilder("url", ModelType.STRING, false)
            .setAllowExpression(false)
            .build();

    protected static final SimpleAttributeDefinition CHANGE_TO =
            new SimpleAttributeDefinitionBuilder(OP, ModelType.STRING, false)
            .setAllowExpression(false)
            .setValidator(new EnumValidator(changeToEnum.class,  false, false))
            .build();

    public static OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("manage-overlay", AuthServerDefinition.rscDescriptionResolver)
            .addParameter(URL)
            .addParameter(CHANGE_TO)
            .build();

    private ManageOverlayHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode mn) throws OperationFailedException {
        System.out.println("Executing!!!!");
        PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, "main-auth-server.war-keycloak-overlay"));
        //PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement("path", "user.dir"));
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();

        Resource resource = context.readResourceFromRoot(pathAddress);
        Set<PathElement> childAddrs = rootResourceRegistration.getChildAddresses(pathAddress);
        Set<String> children = rootResourceRegistration.getChildNames(pathAddress);


        System.out.println("***************");
        System.out.println("childAddrs=" + childAddrs);
        System.out.println("children=" + children);
        System.out.println("model=" + resource.getModel());
        System.out.println("children=" + resource.getChildrenNames("deployment"));
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

}
