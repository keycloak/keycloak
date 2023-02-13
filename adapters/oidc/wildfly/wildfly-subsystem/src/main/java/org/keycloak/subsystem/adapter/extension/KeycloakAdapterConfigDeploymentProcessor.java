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

import static org.keycloak.subsystem.adapter.extension.Elytron.isElytronEnabled;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.keycloak.adapters.elytron.KeycloakConfigurationServletListener;
import org.keycloak.subsystem.adapter.logging.KeycloakLogger;

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

    // not sure if we need this yet, keeping here just in case
    protected void addSecurityDomain(DeploymentUnit deploymentUnit, KeycloakAdapterConfigService service) {
        if (!service.isSecureDeployment(deploymentUnit)) {
            return;
        }
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) return;
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) return;

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig == null || !loginConfig.getAuthMethod().equalsIgnoreCase("KEYCLOAK")) {
            return;
        }

        webMetaData.setSecurityDomain("keycloak");
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        KeycloakAdapterConfigService service = KeycloakAdapterConfigService.getInstance();
        if (service.isSecureDeployment(deploymentUnit) && service.isDeploymentConfigured(deploymentUnit)) {
            addKeycloakAuthData(phaseContext, service);
        }

        addConfigurationListener(phaseContext);

        // FYI, Undertow Extension will find deployments that have auth-method set to KEYCLOAK

        // todo notsure if we need this
        // addSecurityDomain(deploymentUnit, service);
    }

    private void addKeycloakAuthData(DeploymentPhaseContext phaseContext, KeycloakAdapterConfigService service) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            throw new DeploymentUnitProcessingException("WarMetaData not found for " + deploymentUnit.getName() + ".  Make sure you have specified a WAR as your secure-deployment in the Keycloak subsystem.");
        }

        addJSONData(service.getJSON(deploymentUnit), warMetaData);
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig == null) {
            loginConfig = new LoginConfigMetaData();
            webMetaData.setLoginConfig(loginConfig);
        }
        loginConfig.setAuthMethod("KEYCLOAK");
        loginConfig.setRealmName(service.getRealmName(deploymentUnit));
        KeycloakLogger.ROOT_LOGGER.deploymentSecured(deploymentUnit.getName());
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
        param.setParamName(AUTH_DATA_PARAM_NAME);
        param.setParamValue(json);
        contextParams.add(param);

        webMetaData.setContextParams(contextParams);
    }

    private void addConfigurationListener(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }

        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig == null) {
            return;
        }
        if (!loginConfig.getAuthMethod().equals("KEYCLOAK")) {
            return;
        }

        if (isElytronEnabled(phaseContext)) {
            ListenerMetaData listenerMetaData = new ListenerMetaData();

            listenerMetaData.setListenerClass(KeycloakConfigurationServletListener.class.getName());

            webMetaData.getListeners().add(listenerMetaData);
        }
    }

    @Override
    public void undeploy(DeploymentUnit du) {

    }
}
