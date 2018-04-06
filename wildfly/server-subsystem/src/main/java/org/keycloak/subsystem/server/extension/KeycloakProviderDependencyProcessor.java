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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.AbstractVirtualFileFilterWithAttributes;
import org.keycloak.provider.KeycloakDeploymentInfo;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakProviderDependencyProcessor implements DeploymentUnitProcessor {
    private static final ModuleIdentifier KEYCLOAK_COMMON = ModuleIdentifier.create("org.keycloak.keycloak-common");
    private static final ModuleIdentifier KEYCLOAK_CORE = ModuleIdentifier.create("org.keycloak.keycloak-core");
    private static final ModuleIdentifier KEYCLOAK_SERVER_SPI = ModuleIdentifier.create("org.keycloak.keycloak-server-spi");
    private static final ModuleIdentifier KEYCLOAK_SERVER_SPI_PRIVATE = ModuleIdentifier.create("org.keycloak.keycloak-server-spi-private");
    private static final ModuleIdentifier KEYCLOAK_JPA = ModuleIdentifier.create("org.keycloak.keycloak-model-jpa");
    private static final ModuleIdentifier JAXRS = ModuleIdentifier.create("javax.ws.rs.api");
    private static final ModuleIdentifier RESTEASY = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxrs");
    private static final ModuleIdentifier APACHE = ModuleIdentifier.create("org.apache.httpcomponents");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        KeycloakAdapterConfigService config = KeycloakAdapterConfigService.INSTANCE;
        String deploymentName = deploymentUnit.getName();

        if (config.isKeycloakServerDeployment(deploymentName)) {
            return;
        }

        KeycloakDeploymentInfo info = getKeycloakProviderDeploymentInfo(deploymentUnit);
        if (info.hasServices()) {
            final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            final ModuleLoader moduleLoader = Module.getBootModuleLoader();
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_COMMON, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_CORE, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_SERVER_SPI, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_SERVER_SPI_PRIVATE, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAXRS, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, RESTEASY, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, APACHE, false, false, false, false));
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_JPA, false, false, false, false));
        }
    }

    public KeycloakProviderDependencyProcessor() {
        super();
    }

    public static KeycloakDeploymentInfo getKeycloakProviderDeploymentInfo(DeploymentUnit du) {
        KeycloakDeploymentInfo info = KeycloakDeploymentInfo.create();
        info.name(du.getName());

        final ResourceRoot resourceRoot = du.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot != null) {
            final VirtualFile deploymentRoot = resourceRoot.getRoot();
            if (deploymentRoot != null && deploymentRoot.exists()) {
                if (deploymentRoot.getChild("META-INF/keycloak-themes.json").exists() && deploymentRoot.getChild("theme").exists()) {
                    info.themes();
                }

                if (deploymentRoot.getChild("theme-resources").exists()) {
                    info.themeResources();
                }

                VirtualFile services = deploymentRoot.getChild("META-INF/services");
                if(services.exists()) {
                    try {
                        List<VirtualFile> archives = services.getChildren(new AbstractVirtualFileFilterWithAttributes() {
                            @Override
                            public boolean accepts(VirtualFile file) {
                                return file.getName().startsWith("org.keycloak");
                            }
                        });
                        if (!archives.isEmpty()) {
                            info.services();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return info;
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
