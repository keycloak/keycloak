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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Utility methods that help assemble and start an auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class AuthServerUtil {

    private static final ModuleIdentifier KEYCLOAK_SUBSYSTEM = ModuleIdentifier.create("org.keycloak.keycloak-subsystem");

    private final String deploymentName;
    private final Module subsysModule;
    private final String keycloakVersion;
    private final boolean isAuthServerExploded;
    private final URI authServerUri;

    AuthServerUtil(ModelNode operation) {
        this.deploymentName = getDeploymentName(operation);
        this.subsysModule = findSubsysModule();
        this.keycloakVersion = subsysModule.getProperty("keycloak-version");
        this.isAuthServerExploded = Boolean.parseBoolean(subsysModule.getProperty("auth-server-exploded"));
        this.authServerUri = findAuthServerUri();
    }

    String getDeploymentName() {
        return this.deploymentName;
    }

    private Module findSubsysModule() {
        try {
            return Module.getModuleFromCallerModuleLoader(KEYCLOAK_SUBSYSTEM);
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Can't find Keycloak subsystem.", e);
        }
    }

    private URI findAuthServerUri() throws IllegalStateException {
        try {
            URL subsysJar = this.subsysModule.getExportedResource("keycloak-subsystem-" + this.keycloakVersion + ".jar");
            File subsysDir = new File(subsysJar.toURI()).getParentFile();
            File authServerDir = new File(subsysDir, "auth-server");
            if (this.isAuthServerExploded) {
                return authServerDir.toURI();
            } else {
                return new File(authServerDir, "keycloak-server-" + keycloakVersion + ".war").toURI();
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    void addStepToUploadAuthServer(OperationContext context, boolean isEnabled) throws OperationFailedException {
        PathAddress deploymentAddress = deploymentAddress(deploymentName);
        ModelNode op = Util.createOperation(ADD, deploymentAddress);
        op.get(ENABLED).set(isEnabled);
        op.get(PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml

        if (authServerUri == null) {
            throw new OperationFailedException("Keycloak Auth Server WAR not found in keycloak-subsystem module");
        }

        op.get(CONTENT).add(makeContentItem());

        context.addStep(op, getHandler(context, deploymentAddress, ADD), OperationContext.Stage.MODEL);
    }

    private ModelNode makeContentItem() throws OperationFailedException {
        ModelNode contentItem = new ModelNode();

        if (this.isAuthServerExploded) {
            String urlString = new File(authServerUri).getAbsolutePath();
            contentItem.get(PATH).set(urlString);
            contentItem.get(ARCHIVE).set(false);
        } else {
            String urlString = authServerUri.toString();
            contentItem.get(URL).set(urlString);
        }

        return contentItem;
    }

    static void addStepToRedeployAuthServer(OperationContext context, String deploymentName) {
        addDeploymentAction(context, REDEPLOY, deploymentName);
    }

    static void addStepToUndeployAuthServer(OperationContext context, String deploymentName) {
        addDeploymentAction(context, UNDEPLOY, deploymentName);
    }

    static void addStepToDeployAuthServer(OperationContext context, String deploymentName) {
        addDeploymentAction(context, DEPLOY, deploymentName);
    }

    private static void addDeploymentAction(OperationContext context, String operation, String deploymentName) {
        if (!context.isNormalServer()) return;
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

    static String getAuthServerName(ModelNode operation) {
        PathAddress pathAddr = getPathAddress(operation);
        return pathAddr.getElement(pathAddr.size() - 1).getValue();
    }

    static PathAddress getPathAddress(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(ADDRESS));
    }

    static PathAddress getOverlayAddress(String overlayName) {
        return PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, overlayName));
    }

    static String getOverlayName(ModelNode operation) {
        return AuthServerUtil.getAuthServerName(operation) + "-keycloak-overlay";
    }

    static boolean isOverlayExists(OperationContext context, String overlayName, PathAddress address) {
        Resource resource = context.readResourceFromRoot(address);
        return resource.getChildrenNames(DEPLOYMENT_OVERLAY).contains(overlayName);
    }

}
