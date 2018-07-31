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

package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public abstract class KeycloakDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier KEYCLOAK_JBOSS_CORE_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-jboss-adapter-core");
    private static final ModuleIdentifier KEYCLOAK_CORE_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-adapter-core");
    private static final ModuleIdentifier KEYCLOAK_CORE = ModuleIdentifier.create("org.keycloak.keycloak-core");
    private static final ModuleIdentifier KEYCLOAK_COMMON = ModuleIdentifier.create("org.keycloak.keycloak-common");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!KeycloakAdapterConfigService.getInstance().isSecureDeployment(deploymentUnit)) {
            WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
            if (warMetaData == null) {
                return;
            }
            JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
            if (webMetaData == null) {
                return;
            }
            LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
            if (loginConfig == null) return;
            if (loginConfig.getAuthMethod() == null) return;
            if (!loginConfig.getAuthMethod().equals("KEYCLOAK")) return;
        }

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addCommonModules(moduleSpecification, moduleLoader);
        addPlatformSpecificModules(phaseContext, moduleSpecification, moduleLoader);
    }
    
    private void addCommonModules(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        // ModuleDependency(ModuleLoader moduleLoader, ModuleIdentifier identifier, boolean optional, boolean export, boolean importServices, boolean userSpecified)
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_JBOSS_CORE_ADAPTER, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_CORE_ADAPTER, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_CORE, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_COMMON, false, false, false, false));
    }

    abstract protected void addPlatformSpecificModules(DeploymentPhaseContext phaseContext, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader);

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
