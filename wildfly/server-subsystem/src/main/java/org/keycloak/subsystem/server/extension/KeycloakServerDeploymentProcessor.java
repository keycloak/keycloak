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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * DUP responsible for setting the web context of a Keycloak auth server and
 * passing the Keycloak configuration to the Keycloak server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakServerDeploymentProcessor implements DeploymentUnitProcessor {

    // This param name is defined again in Keycloak Services class
    // org.keycloak.services.resources.KeycloakApplication.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keyclaok Services module.
    public static final String KEYCLOAK_CONFIG_PARAM_NAME = "org.keycloak.server-subsystem.Config";
    
    private static final ServiceName cacheContainerService = ServiceName.of("jboss", "infinispan", "keycloak");
    
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        KeycloakAdapterConfigService configService = KeycloakAdapterConfigService.INSTANCE;
        String deploymentName = deploymentUnit.getName();

        if (!configService.isKeycloakServerDeployment(deploymentName)) {
            return;
        }

        final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        String webContext = configService.getWebContext();
        if (webContext == null) {
            throw new DeploymentUnitProcessingException("Can't determine web context/module for Keycloak Server");
        }
        description.setModuleName(webContext);

        addInfinispanCaches(phaseContext);
        addConfiguration(deploymentUnit, configService);
    }
    
    private void addConfiguration(DeploymentUnit deploymentUnit, KeycloakAdapterConfigService configService) throws DeploymentUnitProcessingException {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            throw new DeploymentUnitProcessingException("WarMetaData not found for KeycloakServer.");
        }
        
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<ParamValueMetaData>();
        }

        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(KEYCLOAK_CONFIG_PARAM_NAME);
        param.setParamValue(configService.getConfig().toString());
        contextParams.add(param);

        webMetaData.setContextParams(contextParams);
    }

    private void addInfinispanCaches(DeploymentPhaseContext context) {
        if (context.getServiceRegistry().getService(cacheContainerService) != null) {
            ServiceTarget st = context.getServiceTarget();
            st.addDependency(cacheContainerService);
            st.addDependency(cacheContainerService.append("realms"));
            st.addDependency(cacheContainerService.append("users"));
            st.addDependency(cacheContainerService.append("sessions"));
            st.addDependency(cacheContainerService.append("offlineSessions"));
            st.addDependency(cacheContainerService.append("loginFailures"));
            st.addDependency(cacheContainerService.append("work"));
            st.addDependency(cacheContainerService.append("authorization"));;
        }
    }

    @Override
    public void undeploy(DeploymentUnit du) {
    }
}
