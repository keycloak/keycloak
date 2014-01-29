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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * Pass authentication data (keycloak.json) as a servlet context param so it can be read by the KeycloakServletExtension.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakAdapterConfigDeploymentProcessor implements DeploymentUnitProcessor {
    // This param name is defined again in Keycloak Undertow Integration class
    // org.keycloak.adapters.undertow.KeycloakServletExtension.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keyclaok Undertow Integration.
    public static final String AUTH_DATA_PARAM_NAME = "org.keycloak.json.adapterConfig";

    public static final Phase PHASE = Phase.INSTALL;
    // Seems wise to have this run after INSTALL_WAR_DEPLOYMENT
    public static final int PRIORITY = Phase.INSTALL_WAR_DEPLOYMENT + 1;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        String deploymentName = deploymentUnit.getName();

        KeycloakAdapterConfigService service = KeycloakAdapterConfigService.find(phaseContext.getServiceRegistry());
        if (service.isKeycloakDeployment(deploymentName)) {
            addKeycloakAuthData(phaseContext, deploymentName, service);
        }
    }

    private void addKeycloakAuthData(DeploymentPhaseContext phaseContext, String deploymentName, KeycloakAdapterConfigService service) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        //TODO: Find context root properly
        String resourceName = deploymentName.substring(0, deploymentName.lastIndexOf('.'));

        addJSONData(service.getJSON(deploymentName, resourceName), warMetaData);
        //addJSONData(getJSON(), warMetaData);
    }

    // TODO: remove this.
    private String getJSON() {
        return "{\n" +
"  \"realm\": \"demo\",\n" +
"  \"realm-public-key\": \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB\",\n" +
"  \"auth-server-url\": \"http://localhost:8080/auth\",\n" +
"  \"ssl-not-required\": true,\n" +
"  \"resource\": \"customer-portal-subsys\",\n" +
"  \"credentials\": {\n" +
"    \"password\": \"password\"\n" +
"  },\n" +
"  \"use-resource-role-mappings\": false,\n" +
"  \"enable-cors\": false,\n" +
"  \"cors-max-age\": -1,\n" +
"  \"expose-token\": false,\n" +
"  \"bearer-only\": false\n" +
"}";
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

    @Override
    public void undeploy(DeploymentUnit du) {

    }

}
