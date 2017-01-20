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
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;

/**
 * Utility methods that help assemble and start an auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class ServerUtil {

    private static final ModuleIdentifier KEYCLOAK_SUBSYSTEM = ModuleIdentifier.create("org.keycloak.keycloak-server-subsystem");

    private final String deploymentName;
    private final Module subsysModule;
    private final URI serverWar;

    ServerUtil(ModelNode operation) {
        this.deploymentName = getDeploymentName(operation);
        this.subsysModule = findSubsysModule();
        this.serverWar = findServerWarUri();
    }

    private Module findSubsysModule() {
        try {
            return Module.getModuleFromCallerModuleLoader(KEYCLOAK_SUBSYSTEM);
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Can't find Keycloak subsystem.", e);
        }
    }

    private URI findServerWarUri() throws IllegalStateException {
        try {
            URL subsysResource = this.subsysModule.getExportedResource("module.xml");
            File subsysDir = new File(subsysResource.toURI()).getParentFile();
            File serverWarDir = new File(subsysDir, "server-war");
            return serverWarDir.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    void addStepToUploadServerWar(OperationContext context) throws OperationFailedException {
        PathAddress deploymentAddress = deploymentAddress(deploymentName);
        ModelNode op = Util.createOperation(ADD, deploymentAddress);

        // this is required for deployment to take place
        op.get(ENABLED).set(true);

        // prevents writing this deployment out to standalone.xml
        op.get(PERSISTENT).set(false);

        // Owner attribute is valid starting with WidlFly 9.  Ignored in WildFly 8
        op.get("owner").set(new ModelNode().add("subsystem", KeycloakExtension.SUBSYSTEM_NAME));

        if (serverWar == null) {
            throw new OperationFailedException("Keycloak Server WAR not found in keycloak-server-subsystem module");
        }

        op.get(CONTENT).add(makeContentItem());

        context.addStep(op, getHandler(context, deploymentAddress, ADD), OperationContext.Stage.MODEL);
    }

    private ModelNode makeContentItem() throws OperationFailedException {
        ModelNode contentItem = new ModelNode();

        String urlString = new File(serverWar).getAbsolutePath();
        contentItem.get(PATH).set(urlString);
        contentItem.get(ARCHIVE).set(false);

        return contentItem;
    }

    static void addStepToRedeployServerWar(OperationContext context, String deploymentName) {
        addDeploymentAction(context, REDEPLOY, deploymentName);
    }

    private static void addDeploymentAction(OperationContext context, String operation, String deploymentName) {
        if (!context.isNormalServer()) {
            return;
        }
        PathAddress deploymentAddress = deploymentAddress(deploymentName);
        ModelNode op = Util.createOperation(operation, deploymentAddress);
        op.get(RUNTIME_NAME).set(deploymentName);
        context.addStep(op, getHandler(context, deploymentAddress, operation), OperationContext.Stage.MODEL);
    }

    private static PathAddress deploymentAddress(String deploymentName) {
        return PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, deploymentName));
    }

    static OperationStepHandler getHandler(OperationContext context, PathAddress address, String opName) {
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        return rootResourceRegistration.getOperationHandler(address, opName);
    }

    static String getDeploymentName(ModelNode operation) {
        String deploymentName = Util.getNameFromAddress(operation.get(ADDRESS));
        if (!deploymentName.toLowerCase().endsWith(".war")) {
            deploymentName += ".war";
        }

        return deploymentName;
    }
}
