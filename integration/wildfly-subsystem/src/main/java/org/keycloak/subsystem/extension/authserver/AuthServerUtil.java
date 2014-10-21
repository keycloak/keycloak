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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.Resource;
import org.jboss.modules.filter.PathFilter;

/**
 * Utility methods that help assemble and start an auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class AuthServerUtil {

    private static final ModuleIdentifier KEYCLOAK_SUBSYSTEM = ModuleIdentifier.create("org.keycloak.keycloak-wildfly-subsystem");

    private final String authServerName;
    private final PathAddress pathAddress;
    private String deploymentName;

    //private String overlayName;
    private Module subsysModule;
    private String keycloakVersion;

    //private File overlaysDir;
    private URL authServerUrl = null;
    //private URL serverConfig = null;
    //private Set<URL> spiUrls = new HashSet<URL>();

    AuthServerUtil(ModelNode operation) {
        this.authServerName = getAuthServerName(operation);
        this.pathAddress = getPathAddress(operation);
        this.deploymentName = getDeploymentName(operation);

        //this.overlayName = deploymentName + "-keycloak-overlay";
        setModule();
        findAuthServerUrl();
        //findSpiUrls();

        System.out.println("&&&&& " + authServerName + " authServerUrl=" + authServerUrl);
//        System.out.println("&&&&& " + authServerName + " spiUrls=" + spiUrls);
//        System.out.println("&&&&& " + authServerName + " serverConfig=" + serverConfig);
    }

    String getDeploymentName() {
        return this.deploymentName;
    }

    private void setModule() {
        try {
            this.subsysModule = Module.getModuleFromCallerModuleLoader(KEYCLOAK_SUBSYSTEM);
            this.keycloakVersion = subsysModule.getProperty("keycloak-version");
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Can't find Keycloak subsystem.", e);
        }
    }

    /*private void findSpiUrls() throws IllegalStateException {
        try {
            Iterator<Resource> rscIterator = this.subsysModule.iterateResources(new PathFilter() {
                @Override
                public boolean accept(String string) {
                    return string.equals(AuthServerUtil.this.authServerName);
                }
            });

            while (rscIterator.hasNext()) {
                Resource rsc = rscIterator.next();
                System.out.println("rsc.getName()=" + rsc.getName());
                URL url = rsc.getURL();

                if (isJar(rsc)) {
                    this.spiUrls.add(url);
                }
                if (isServerConfig(rsc)) {
                    this.serverConfig = url;
                }
            }
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        }
    }*/

    private void findAuthServerUrl() throws IllegalStateException {
        try {
            Iterator<org.jboss.modules.Resource> rscIterator = this.subsysModule.iterateResources(new PathFilter() {
                @Override
                public boolean accept(String string) {
                    return string.equals("");
                }
            });

            while (rscIterator.hasNext()) {
                Resource rsc = rscIterator.next();
                System.out.println("rsc.getName()=" + rsc.getName());
                URL url = rsc.getURL();
                String parent = "";
                try {
                    parent = new File(url.toURI()).getParent();
                } catch (URISyntaxException e) {
                    continue;
                } catch (IllegalArgumentException e) {
                    continue;
                }

                if (isAuthServer(rsc, parent)) {
                    this.authServerUrl = url;
                    //File mainDir = new File(parent).getParentFile();
                    //this.overlaysDir = new File(mainDir, "overlays");
                    break;
                }
            }
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isAuthServer(Resource rsc, String parent) {
        return rsc.getName().equals("keycloak-server-" + keycloakVersion + ".war")
                && parent.toLowerCase().endsWith("auth-server");
    }

    /*private boolean isServerConfig(Resource rsc) {
        return rsc.getName().endsWith("/keycloak-server.json");
    }

    private boolean isJar(Resource rsc) {
        return rsc.getName().toLowerCase().endsWith(".jar");
    }

    boolean serverOverlayDirExists() {
        return new File(overlaysDir, authServerName).exists();
    }

    private boolean hasOverlays() {
        return (this.serverConfig != null) || (!this.spiUrls.isEmpty());
    }*/

    void addStepToUploadAuthServer(OperationContext context, boolean isEnabled) throws OperationFailedException {
        PathAddress deploymentAddress = deploymentAddress();
        ModelNode op = Util.createOperation(ADD, deploymentAddress);
        op.get(ENABLED).set(isEnabled);
        op.get(PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml

        if (authServerUrl == null) {
            throw new OperationFailedException("Keycloak Auth Server WAR not found in keycloak-wildfly-subsystem module");
        }

        String urlString = authServerUrl.toExternalForm();
        ModelNode contentItem = new ModelNode();
        contentItem.get(URL).set(urlString);
        op.get(CONTENT).add(contentItem);

        System.out.println("*** add auth server operation");
        System.out.println(op.toString());
        context.addStep(op, getHandler(context, deploymentAddress, ADD), OperationContext.Stage.MODEL);

        /*File authServerOverlaysDir = new File(this.overlaysDir, authServerName);
        System.out.println("authServerOverlaysDir" + authServerOverlaysDir.getAbsolutePath());
        if (!authServerOverlaysDir.exists()) {
            authServerOverlaysDir.mkdir();
            addOverlay(context);
            linkToDeployment(context);
        }*/
    }

    void addStepToRedeployAuthServer(OperationContext context) {
        addDeploymentAction(context, REDEPLOY);
    }

    void addStepToUndeployAuthServer(OperationContext context) {
        addDeploymentAction(context, UNDEPLOY);
    }

    void addStepToDeployAuthServer(OperationContext context) {
        addDeploymentAction(context, DEPLOY);
    }

    private void addDeploymentAction(OperationContext context, String operation) {
        PathAddress deploymentAddress = deploymentAddress();
        ModelNode op = Util.createOperation(operation, deploymentAddress);
        op.get(RUNTIME_NAME).set(deploymentName);
        System.out.println(">>>> operation=" + operation);
        System.out.println(op.toString());
        context.addStep(op, getHandler(context, deploymentAddress, operation), OperationContext.Stage.MODEL);
    }

    private PathAddress deploymentAddress() {
        return PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, deploymentName));
    }

    /*void addStepsToAssembleOverlay(OperationContext context) throws OperationFailedException {
        if (hasOverlays()) {
            addOverlay(context);
            addKeycloakServerJson(context);
            addSpiJars(context);
            linkToDeployment(context);
        }
        removeOverlayDir();
    }

    private void removeOverlayDir() {
        // TODO implement as operation
    }

    private void addOverlay(OperationContext context) throws OperationFailedException {
        if (!hasOverlays()) return;

        PathAddress overlayAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, overlayName));

        ModelNode addOp = Util.createOperation(ADD, overlayAddress);
        //addOp.get(PERSISTENT).set(false);

        addRollbackFalse(addOp);
        System.out.println("*** add overlay operation");
        System.out.println(addOp.toString());
        context.addStep(addOp, getAddHandler(context, overlayAddress), OperationContext.Stage.MODEL);
    }

    private void addKeycloakServerJson(OperationContext context) throws OperationFailedException {
        if (this.serverConfig == null) {
            return;
        }

        addOveralyContent(context, this.serverConfig, "/WEB-INF/classes/META-INF/keycloak-server.json");
        addChangeToOperation(context, this.serverConfig, ManageOverlayHandler.changeToEnum.deployed);
    }

    private void addSpiJars(OperationContext context) throws OperationFailedException {
        if (this.spiUrls.isEmpty()) {
            return;
        }

        for (URL source : this.spiUrls) {
            try {
                String fileName = new java.io.File(source.toURI()).getName();
                addOveralyContent(context, source, "/WEB-INF/lib/" + fileName);
            } catch (URISyntaxException e) {
                throw new OperationFailedException(e);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(e);
            }
        }
    }

    private void linkToDeployment(OperationContext context) throws OperationFailedException {
        if (!hasOverlays()) return;

        PathAddress linkAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, overlayName),
                PathElement.pathElement(DEPLOYMENT, deploymentName));
        ModelNode op = Util.createOperation(ADD, linkAddress);

        addRollbackFalse(op);
        System.out.println("*** link to deployment operation");
        System.out.println(op.toString());
        context.addStep(op, getAddHandler(context, linkAddress), OperationContext.Stage.MODEL);
    }

    private void addOveralyContent(OperationContext context, URL source, String destination) throws OperationFailedException {
        PathAddress contentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, overlayName),
                PathElement.pathElement(CONTENT, destination));
        ModelNode op = Util.createOperation(ADD, contentAddress);

        ModelNode contentItem = new ModelNode();
        contentItem.get(URL).set(source.toExternalForm());
        op.get(CONTENT).set(contentItem);

        addRollbackFalse(op);
        System.out.println("*** add content operation");
        System.out.println(op.toString());

        context.addStep(op, getAddHandler(context, contentAddress), OperationContext.Stage.MODEL);
    }

    private void addChangeToOperation(OperationContext context, URL source, ManageOverlayHandler.changeToEnum changeTo) {
        ModelNode op = Util.createOperation(ManageOverlayHandler.OP, this.pathAddress);
        op.get(ManageOverlayHandler.URL.getName()).set(source.toExternalForm());
        op.get(ManageOverlayHandler.CHANGE_TO.getName()).set(changeTo.toString());

        System.out.println("************change-to operation********************");
        System.out.println(op.toString());
        context.addStep(op, ManageOverlayHandler.INSTANCE, OperationContext.Stage.RUNTIME, false);
    }*/

    private OperationStepHandler getHandler(OperationContext context, PathAddress address, String opName) {
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        return rootResourceRegistration.getOperationHandler(address, opName);
        //return new IgnoreIfResourceExistsHandler(handler);
    }

    private void addRollbackFalse(ModelNode modelNode) {
        modelNode.get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);
    }

    static String getDeploymentName(ModelNode operation) {
        String deploymentName = Util.getNameFromAddress(operation.get(ADDRESS));
        System.out.println("*** authServerName=" + deploymentName);
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

}
