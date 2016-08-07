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

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.AbstractVirtualFileFilterWithAttributes;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.ProviderManagerRegistry;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakProviderDeploymentProcessor implements DeploymentUnitProcessor {

    AttachmentKey<ProviderManager> ATTACHMENT_KEY = AttachmentKey.create(ProviderManager.class);

    private static final Logger logger = Logger.getLogger(KeycloakProviderDeploymentProcessor.class);
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        KeycloakAdapterConfigService config = KeycloakAdapterConfigService.INSTANCE;
        String deploymentName = deploymentUnit.getName();

        if (config.isKeycloakServerDeployment(deploymentName)) {
            return;
        }

        if (!KeycloakProviderDependencyProcessor.isKeycloakProviderDeployment(deploymentUnit)) return;

        logger.infof("Deploying Keycloak provider: {0}", deploymentUnit.getName());
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ProviderManager pm = new ProviderManager(module.getClassLoader());
        ProviderManagerRegistry.SINGLETON.deploy(pm);
        deploymentUnit.putAttachment(ATTACHMENT_KEY, pm);



    }

    public KeycloakProviderDeploymentProcessor() {
        super();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        ProviderManager pm = context.getAttachment(ATTACHMENT_KEY);
        if (pm != null) {
            logger.infof("Undeploying Keycloak provider: {0}", context.getName());
            ProviderManagerRegistry.SINGLETON.undeploy(pm);
            context.removeAttachment(ATTACHMENT_KEY);
        }
    }
}
