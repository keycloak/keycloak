/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.subsystem.extension;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class KeycloakDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier KEYCLOAK_WILDFLY_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-wildfly-adapter");
    private static final ModuleIdentifier KEYCLOAK_UNDERTOW_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-undertow-adapter");
    private static final ModuleIdentifier KEYCLOAK_JBOSS_CORE_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-jboss-adapter-core");
    private static final ModuleIdentifier KEYCLOAK_CORE_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-adapter-core");
    private static final ModuleIdentifier KEYCLOAK_CORE = ModuleIdentifier.create("org.keycloak.keycloak-core");
    private static final ModuleIdentifier APACHE_HTTPCOMPONENTS = ModuleIdentifier.create("org.apache.httpcomponents");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        String deploymentName = deploymentUnit.getName();
        KeycloakAdapterConfigService service = KeycloakAdapterConfigService.find(phaseContext.getServiceRegistry());

        if (service.isKeycloakDeployment(deploymentName)) {
            addModules(deploymentUnit);
        }

        for (ResourceRoot root : deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS)) {
            System.out.println("*** resource root=" + root);
        }


    }

    private void addProvider(DeploymentUnit deploymentUnit) throws IOException, URISyntaxException {
        System.out.println("#2");
        deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, providerRoot());
        System.out.println("#4");
    }

    private ResourceRoot providerRoot() throws IOException, URISyntaxException {
        System.out.println("#3");
        URI uri = new URI("file:/C:/GitHub/keycloak-temp/keycloak-appliance-dist-all-1.1.0-Alpha1-SNAPSHOT/keycloak/modules/system/layers/base/org/keycloak/keycloak-auth-server/main/./federation-properties-example.jar");
        VirtualFile archive = VFS.getChild(uri);
        Closeable closeable = VFS.mountZip(archive.getPhysicalFile(), archive, TempFileProviderService.provider());
        return new ResourceRoot(archive.getName(), archive, new MountHandle(closeable));
    }

    private void addModules(DeploymentUnit deploymentUnit) {
        System.out.println("**************************");
        System.out.println("* Adding Keycloak dependencies to " + deploymentUnit.getName());
        System.out.println("**************************");
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_WILDFLY_ADAPTER, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_UNDERTOW_ADAPTER, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_JBOSS_CORE_ADAPTER, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_CORE_ADAPTER, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_CORE, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, APACHE_HTTPCOMPONENTS, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
