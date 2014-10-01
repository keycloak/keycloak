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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
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
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class AuthServerUtil {

    private static final ModuleIdentifier KEYCLOAK_SUBSYSTEM = ModuleIdentifier.create("org.keycloak.keycloak-wildfly-subsystem");

    private static URL authServerUrl = null;

    private static String defaultAuthServerJson = "";

    static String getDefaultAuthServerJson() {
        if (authServerUrl == null) getWarUrl();
        return defaultAuthServerJson;
    }

    // Can return the URL, null, or throw IllegalStateException
    // This also finds the defaultAuthServerJson and sets the instance var for it.
    private static URL getWarUrl() throws IllegalStateException {
        if (authServerUrl != null) { // only need to find this once
            return authServerUrl;
        }

        Module module;
        try {
            module = Module.getModuleFromCallerModuleLoader(KEYCLOAK_AUTH_SERVER);
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Keycloak Auth Server not installed as a module.", e);
        }

        URL warUrl = null;
        try {
            java.util.Iterator<org.jboss.modules.Resource> rscIterator = module.iterateResources(new PathFilter() {
                @Override
                public boolean accept(String string) {
                    return true;
                }
            });

            // There should be only one war resource, the auth server
            while (rscIterator.hasNext()) {
                Resource rsc = rscIterator.next();
                System.out.println("rsc.getName()=" + rsc.getName());
                URL url = rsc.getURL();
                if (url.toExternalForm().toLowerCase().endsWith(".war")) {
                    warUrl = url;
                    setDefaultAuthServerJson(rsc);
                    break;
                }
            }
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        authServerUrl = warUrl;
        System.out.println("&&&&& authServerUrl=" + authServerUrl);
        return authServerUrl;
    }

    // return deploymentName this will be started under
    static String addStepToStartAuthServer(OperationContext context, ModelNode operation) throws OperationFailedException {

        PathAddress authServerAddr = PathAddress.pathAddress(operation.get(ADDRESS));
        String deploymentName = authServerAddr.getElement(1).getValue();
        if (!deploymentName.toLowerCase().endsWith(".war")) {
            deploymentName += ".war";
        }

        PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, deploymentName));
        ModelNode op = Util.createOperation(ADD, deploymentAddress);
        op.get(ENABLED).set(true);
        op.get(PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml

        URL warUrl = null;
        try {
            warUrl = getWarUrl();
        } catch (IllegalStateException e) {
            throw new OperationFailedException(e);
        }

        if (warUrl == null) {
            throw new OperationFailedException("Keycloak Auth Server WAR not found in keycloak-auth-server module");
        }

        String urlString = warUrl.toExternalForm();
        System.out.println(warUrl);
        ModelNode contentItem = new ModelNode();
        contentItem.get(URL).set(urlString);
        op.get(CONTENT).add(contentItem);
        System.out.println("****** operation ************");
        System.out.println(op.toString());
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress, ADD);
        context.addStep(op, handler, OperationContext.Stage.MODEL);

        return deploymentName;
    }

    private static void setDefaultAuthServerJson(Resource rsc) throws IOException {
        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(rsc.openStream());
            JarEntry je;
            while ((je = jarStream.getNextJarEntry()) != null) {
                if (!je.getName().equals("WEB-INF/classes/META-INF/keycloak-server.json")) continue;

                int len = 0;
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((len = jarStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                defaultAuthServerJson = baos.toString();
                return;
            }
        } finally {
            jarStream.close();
        }
    }
}
