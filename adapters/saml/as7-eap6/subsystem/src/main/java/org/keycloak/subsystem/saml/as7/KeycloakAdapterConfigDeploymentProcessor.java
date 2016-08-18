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
package org.keycloak.subsystem.saml.as7;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.saml.jbossweb.SamlAuthenticatorValve;
import org.keycloak.subsystem.saml.as7.logging.KeycloakLogger;
import org.keycloak.subsystem.saml.as7.xml.FormattingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Pass authentication data (keycloak.json) as a servlet context param so it can be read by the KeycloakServletExtension.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakAdapterConfigDeploymentProcessor implements DeploymentUnitProcessor {
    protected Logger log = Logger.getLogger(KeycloakAdapterConfigDeploymentProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
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

        // otherwise
        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();

        try {
            boolean webRequiresKC = loginConfig != null && "KEYCLOAK-SAML".equalsIgnoreCase(loginConfig.getAuthMethod());
            boolean hasSubsystemConfig = Configuration.INSTANCE.isSecureDeployment(deploymentUnit);
            if (hasSubsystemConfig || webRequiresKC) {
                log.debug("Setting up KEYCLOAK-SAML auth method for WAR: " + deploymentUnit.getName());

                // if secure-deployment configuration exists for web app, we force KEYCLOAK-SAML auth method on it
                if (hasSubsystemConfig) {
                    addXMLData(getXML(deploymentUnit), warMetaData);
                    if (loginConfig != null) {
                        loginConfig.setAuthMethod("KEYCLOAK-SAML");
                        //loginConfig.setRealmName(service.getRealmName(deploymentName));
                    } else {
                        log.warn("Failed to set up KEYCLOAK-SAML auth method for WAR: " + deploymentUnit.getName() + " (loginConfig == null)");
                    }
                }
                addValve(webMetaData);
                KeycloakLogger.ROOT_LOGGER.deploymentSecured(deploymentUnit.getName());
            }
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Failed to configure KeycloakSamlExtension from subsystem model", e);
        }
    }

    private String getXML(DeploymentUnit deploymentUnit) throws XMLStreamException {
        ModelNode node = Configuration.INSTANCE.getSecureDeployment(deploymentUnit);
        if (node != null) {
            KeycloakSubsystemParser writer = new KeycloakSubsystemParser();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            XMLExtendedStreamWriter streamWriter = new FormattingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(output));
            try {
                streamWriter.writeStartElement("keycloak-saml-adapter");
                writer.writeSps(streamWriter, node);
                streamWriter.writeEndElement();
            } finally {
                streamWriter.close();
            }
            return new String(output.toByteArray(), Charset.forName("utf-8"));
        }
        return null;
    }

    private void addXMLData(String xml, WarMetaData warMetaData) {
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
        param.setParamName(AdapterConstants.AUTH_DATA_PARAM_NAME);
        param.setParamValue(xml);
        contextParams.add(param);

        webMetaData.setContextParams(contextParams);
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
