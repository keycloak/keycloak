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

package org.keycloak.subsystem.adapter.saml.extension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.saml.elytron.KeycloakConfigurationServletListener;
import org.keycloak.subsystem.adapter.saml.extension.logging.KeycloakLogger;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.staxmapper.FormattingXMLStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.keycloak.subsystem.adapter.saml.extension.Elytron.isElytronEnabled;

/**
 * Pass authentication data (keycloak.json) as a servlet context param so it can be read by the KeycloakServletExtension.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakAdapterConfigDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (Configuration.INSTANCE.getSecureDeployment(deploymentUnit) != null) {
            addKeycloakSamlAuthData(phaseContext);
        }

        addConfigurationListener(phaseContext);
    }

    private void addKeycloakSamlAuthData(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            throw new DeploymentUnitProcessingException("WarMetaData not found for " + deploymentUnit.getName() + ".  Make sure you have specified a WAR as your secure-deployment in the Keycloak subsystem.");
        }

        try {
            addXMLData(getXML(deploymentUnit), warMetaData);
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Failed to configure KeycloakSamlExtension from subsystem model", e);
        }
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
        loginConfig.setAuthMethod("KEYCLOAK-SAML");

        KeycloakLogger.ROOT_LOGGER.deploymentSecured(deploymentUnit.getName());
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

    @Override
    public void undeploy(DeploymentUnit du) {

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
        if (!loginConfig.getAuthMethod().equals("KEYCLOAK-SAML")) {
            return;
        }

        if (isElytronEnabled(phaseContext)) {
            ListenerMetaData listenerMetaData = new ListenerMetaData();

            listenerMetaData.setListenerClass(KeycloakConfigurationServletListener.class.getName());

            webMetaData.getListeners().add(listenerMetaData);
        }
    }
}
