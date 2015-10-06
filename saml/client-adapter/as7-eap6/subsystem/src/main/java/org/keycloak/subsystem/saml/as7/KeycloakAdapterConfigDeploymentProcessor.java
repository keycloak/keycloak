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
package org.keycloak.subsystem.saml.as7;

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
import org.keycloak.adapters.saml.jbossweb.SamlAuthenticatorValve;

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
    public static final String AUTH_DATA_PARAM_NAME = "org.keycloak.saml.adapterConfig";


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        String deploymentName = deploymentUnit.getName();

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

        // otherwise
        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();

        boolean webRequiresKC = loginConfig != null && "KEYCLOAK-SAML".equalsIgnoreCase(loginConfig.getAuthMethod());

        if (webRequiresKC) {
            log.debug("Setting up KEYCLOAK-SAML auth method for WAR: " + deploymentName);
            addValve(webMetaData);
        }
    }

    private void addValve(JBossWebMetaData webMetaData) {
        List<ValveMetaData> valves = webMetaData.getValves();
        if (valves == null) {
            valves = new ArrayList<ValveMetaData>(1);
            webMetaData.setValves(valves);
        }
        ValveMetaData valve = new ValveMetaData();
        valve.setValveClass(SamlAuthenticatorValve.class.getName());
        valve.setModule("org.keycloak.keycloak-saml-as7-adapter");
        //log.info("******* adding Keycloak valve to: " + deploymentName);
        valves.add(valve);
    }

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
