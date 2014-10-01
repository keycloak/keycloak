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

package org.keycloak.subsystem.extension;

import java.io.Closeable;
import java.io.IOException;
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
public class KeycloakStructureProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        String deploymentName = deploymentUnit.getName();
        KeycloakAdapterConfigService service = KeycloakAdapterConfigService.find(phaseContext.getServiceRegistry());

        System.out.println("#0");
        if (service.isKeycloakServerDeployment(deploymentName)) {
            try {
                System.out.println("#1");
                addProvider(deploymentUnit);
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }
    }

    private void addProvider(DeploymentUnit deploymentUnit) throws IOException {
        System.out.println("#2");
        deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, providerRoot());
        System.out.println("#4");
    }

    private ResourceRoot providerRoot() throws IOException {
        System.out.println("#3");
        VirtualFile archive = VFS.getChild("C:\\GitHub\\keycloak-temp\\keycloak-appliance-dist-all-1.1.0-Alpha1-SNAPSHOT\\keycloak\\modules\\system\\layers\\base\\org\\keycloak\\keycloak-auth-server\\main\\federation-properties-example.jar");
        Closeable closeable = VFS.mountZip(archive.getPhysicalFile(), archive, TempFileProviderService.provider());
        return new ResourceRoot(archive.getName(), archive, new MountHandle(closeable));
    }

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
