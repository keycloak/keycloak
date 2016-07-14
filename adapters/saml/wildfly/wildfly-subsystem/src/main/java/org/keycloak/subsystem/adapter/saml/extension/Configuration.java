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

    void updateModel(ModelNode operation, ModelNode model) {
        ModelNode node = config;
        ModelNode addr = operation.get("address");
        for (Property item : addr.asPropertyList()) {
            node = getNodeForAddressElement(node, item);
        }
        node.set(model);
    }

    private ModelNode getNodeForAddressElement(ModelNode node, Property item) {
        String key = item.getValue().asString();
        ModelNode keymodel = node.get(item.getName());
        return keymodel.get(key);
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
