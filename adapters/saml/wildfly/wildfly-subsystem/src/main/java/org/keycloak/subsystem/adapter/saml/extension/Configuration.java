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

import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class Configuration {

    static Configuration INSTANCE = new Configuration();

    private ModelNode config = new ModelNode();

    private Configuration() {
    }

    void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        this.updateModel(operation, model, false);
    }

    void updateModel(final ModelNode operation, final ModelNode model, final boolean checkSingleton) throws OperationFailedException {
        ModelNode node = config;

        final List<Property> addressNodes = operation.get("address").asPropertyList();
        final int lastIndex = addressNodes.size() - 1;
        for (int i = 0; i < addressNodes.size(); i++) {
            Property addressNode = addressNodes.get(i);
            // if checkSingleton is true, we verify if the key for the last element (e.g. SP or IDP) in the address path is already defined
            if (i == lastIndex && checkSingleton) {
                if (node.get(addressNode.getName()).isDefined()) {
                    // found an existing resource, throw an exception
                    throw new OperationFailedException("Duplicate resource: " + addressNode.getName());
                }
            }
            node = node.get(addressNode.getName()).get(addressNode.getValue().asString());
        }
        node.set(model);
    }

    public ModelNode getSecureDeployment(DeploymentUnit deploymentUnit) {
        String name = preferredDeploymentName(deploymentUnit);
        ModelNode secureDeployment = config.get("subsystem").get("keycloak-saml").get(Constants.Model.SECURE_DEPLOYMENT);
        if (secureDeployment.hasDefined(name)) {
            return secureDeployment.get(name);
        }
        return null;
    }
    
    // KEYCLOAK-3273: prefer module name if available
    private String preferredDeploymentName(DeploymentUnit deploymentUnit) {
        String deploymentName = deploymentUnit.getName();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return deploymentName;
        }
        
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            return deploymentName;
        }
        
        String moduleName = webMetaData.getModuleName();
        if (moduleName != null) return moduleName + ".war";
        
        return deploymentName;
    }
}
