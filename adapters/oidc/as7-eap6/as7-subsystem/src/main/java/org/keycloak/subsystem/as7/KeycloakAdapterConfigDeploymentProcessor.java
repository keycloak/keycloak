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
package org.keycloak.subsystem.as7;

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
import org.keycloak.adapters.jbossweb.KeycloakAuthenticatorValve;
import org.keycloak.subsystem.as7.logging.KeycloakLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Pass authentication data (keycloak.json) as a servlet context param so it can be read by the KeycloakServletExtension.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakAdapterConfigDeploymentProcessor implements DeploymentUnitProcessor {
    protected Logger log = Logger.getLogger(KeycloakAdapterConfigDeploymentProcessor.class);

    // This param name is defined again in Keycloak Undertow Integration class
    // org.keycloak.adapters.undertow.KeycloakServletExtension.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keyclaok Undertow Integration.
    public static final String AUTH_DATA_PARAM_NAME = "org.keycloak.json.adapterConfig";


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // if it's not a web-app there's nothing to secure
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        KeycloakAdapterConfigService service = KeycloakAdapterConfigService.getInstance();

        // otherwise
        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();

        boolean hasSubsystemConfig = service.isSecureDeployment(deploymentUnit);
        boolean webRequiresKC = loginConfig != null && "KEYCLOAK".equalsIgnoreCase(loginConfig.getAuthMethod());
        boolean isConfigured = service.isDeploymentConfigured(deploymentUnit);

        if ((hasSubsystemConfig && isConfigured) || webRequiresKC) {
            log.debug("Setting up KEYCLOAK auth method for WAR: " + deploymentUnit.getName());

            // if secure-deployment configuration exists for web app, we force KEYCLOAK auth method on it
            if (hasSubsystemConfig) {
                addJSONData(service.getJSON(deploymentUnit), warMetaData);
                if (loginConfig != null) {
                    loginConfig.setAuthMethod("KEYCLOAK");
                    loginConfig.setRealmName(service.getRealmName(deploymentUnit));
                } else {
                    log.warn("Failed to set up KEYCLOAK auth method for WAR: " + deploymentUnit.getName() + " (loginConfig == null)");
                }
            }
            addValve(webMetaData);
            KeycloakLogger.ROOT_LOGGER.deploymentSecured(deploymentUnit.getName());
        }
    }

    private void addValve(JBossWebMetaData webMetaData) {
        List<ValveMetaData> valves = webMetaData.getValves();
        if (valves == null) {
            valves = new ArrayList<>(1);
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
            contextParams = new ArrayList<>();
        }

        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(AUTH_DATA_PARAM_NAME);
        param.setParamValue(json);
        contextParams.add(param);

        webMetaData.setContextParams(contextParams);
    }

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
