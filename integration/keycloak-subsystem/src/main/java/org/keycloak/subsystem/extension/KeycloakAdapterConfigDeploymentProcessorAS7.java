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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.keycloak.adapters.as7.KeycloakAuthenticatorValve;
import org.keycloak.subsystem.logging.KeycloakLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Pass authentication data (keycloak.json) as a servlet context param so it can be read by the KeycloakServletExtension.
 * This is used for AS7/EAP6.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */

// Note: Even though this class closely resembles the WildFly KeycloakAdapterConfigDeploymentProcessor
//       it can not be easily refactored because the WarMetaData classes are of different types.
public class KeycloakAdapterConfigDeploymentProcessorAS7 implements DeploymentUnitProcessor {
    protected Logger log = Logger.getLogger(KeycloakAdapterConfigDeploymentProcessorAS7.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        String deploymentName = deploymentUnit.getName();

        KeycloakAdapterConfigService service = KeycloakAdapterConfigService.find(phaseContext.getServiceRegistry());
        //log.info("********* CHECK KEYCLOAK DEPLOYMENT: " + deploymentName);
        if (service.isSecureDeployment(deploymentName)) {
            addKeycloakAuthData(phaseContext, deploymentName, service);
            return;
        }

        // else check to see if KEYCLOAK is specified as login config
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) return;
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) return;

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig != null && "KEYCLOAK".equalsIgnoreCase(loginConfig.getAuthMethod())) {
            addValve(webMetaData);
        }
    }

    private void addKeycloakAuthData(DeploymentPhaseContext phaseContext, String deploymentName, KeycloakAdapterConfigService service) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        addJSONData(service.getJSON(deploymentName), warMetaData);
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }
        addValve(webMetaData);

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig == null) {
            loginConfig = new LoginConfigMetaData();
            webMetaData.setLoginConfig(loginConfig);
        }
        loginConfig.setAuthMethod("KEYCLOAK");
        loginConfig.setRealmName(service.getRealmName(deploymentName));
        KeycloakLogger.ROOT_LOGGER.deploymentSecured(deploymentName);
    }

    private void addValve(JBossWebMetaData webMetaData) {
        List<ValveMetaData> valves = webMetaData.getValves();
        if (valves == null) {
            valves = new ArrayList<ValveMetaData>(1);
            webMetaData.setValves(valves);
        }
        ValveMetaData valve = new ValveMetaData();
        valve.setValveClass(KeycloakAuthenticatorValve.class.getName());
        valve.setModule("org.keycloak.keycloak-as7-adapter");
        //log.info("******* adding Keycloak valve to: " + deploymentName);
        valves.add(valve);
    }

    private void addJSONData(String json, WarMetaData warMetaData) {
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
        param.setParamName(KeycloakAdapterConfigDeploymentProcessor.AUTH_DATA_PARAM_NAME);
        param.setParamValue(json);
        contextParams.add(param);

        webMetaData.setContextParams(contextParams);
    }

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
