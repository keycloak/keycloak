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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * This service keeps track of the entire Keycloak management model so as to provide
 * configuration to the Keycloak Server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public final class KeycloakAdapterConfigService {

    static final KeycloakAdapterConfigService INSTANCE = new KeycloakAdapterConfigService();

    static final String DEPLOYMENT_NAME = "keycloak-server.war";
    
    static ModelNode fullConfig = new ModelNode();

    private String webContext;


    private KeycloakAdapterConfigService() {
    }

    void updateConfig(ModelNode operation, ModelNode config) {
        PathAddress address = PathAddress.pathAddress(operation.get(ADDRESS));
        address = address.subAddress(1); // remove root (subsystem=keycloak-server)
        
        ModelNode newConfig = fullConfig.clone();
        ModelNode subNode = newConfig;
        for (PathElement pathElement : address) {
            subNode = subNode.get(pathElement.getKey(), pathElement.getValue());
        }
        
        subNode.set(config.clone());
        
        // remove undefined properties
        for (Property prop : subNode.asPropertyList()) {
            if (!prop.getValue().isDefined()) {
                subNode.remove(prop.getName());
            }
        }
        
        fullConfig = newConfig;
    }
    
    ModelNode getConfig() {
        ModelNode copy = fullConfig.clone();
        //System.out.println("******** BEFORE *************");
        //System.out.println(copy);
        //System.out.println("*****************************");
        copy.remove("web-context");
        massageScheduledTaskInterval(copy);
        massageMasterRealm(copy);
        massageTheme(copy);
        massageSpis(copy);
        //System.out.println("******** JSON *************");
        //System.out.println(copy.resolve().toJSONString(false));
        //System.out.println("**********************");
        return copy;
    }
    
    // The "massage" methods rearrange the model so that everything will
    // be where the Keycloak server's Config interface expects it to be.
    
    private void massageScheduledTaskInterval(ModelNode copy) {
        if (!copy.hasDefined("scheduled-task-interval")) return;
        ModelNode taskInterval = copy.remove("scheduled-task-interval");
        copy.get("scheduled", "interval").set(taskInterval);
    }
    
    private void massageMasterRealm(ModelNode copy) {
        if (!copy.hasDefined("master-realm-name")) return;
        ModelNode master = copy.remove("master-realm-name");
        copy.get("admin", "realm").set(master);
    }
    
    private void massageTheme(ModelNode copy) {
        if (!copy.hasDefined("theme")) return;
        if (!copy.get("theme").hasDefined("defaults")) return;
        
        ModelNode themeDefaults = copy.get("theme", "defaults");
        copy.get("theme").set(themeDefaults);
        
        if (copy.has("theme", "dir")) {
            ModelNode dir = copy.get("theme", "dir");
            copy.get("theme", "folder", "dir").set(dir);
            copy.get("theme").remove("dir");
        }
        
        if (copy.has("theme", "modules")) {
            ModelNode modules = copy.get("theme").remove("modules");
            copy.get("theme", "module", "modules").set(modules);
        }
    }
    
    private void massageSpis(ModelNode copy) {
        if (!copy.hasDefined("spi")) return;
        ModelNode spis = copy.remove("spi");
        
        for (Property prop : spis.asPropertyList()) {
            ModelNode spi = prop.getValue();
            
            if (spi.has("provider")) {
                massageProviders(spi);
            }
            
            if (spi.has("default-provider")) {
                ModelNode defaultProvider = spi.remove("default-provider");
                spi.get("provider").set(defaultProvider);
            }
            
            copy.get(prop.getName()).set(spi);
        }
    }
    
    private void massageProviders(ModelNode spi) {
        if (!spi.hasDefined("provider")) return;
        ModelNode providers = spi.remove("provider");
        for (Property prop : providers.asPropertyList()) {
            ModelNode provider = prop.getValue();
            if (provider.has("properties")) {
                massageProviderProps(provider);
            }
            spi.get(prop.getName()).set(provider);
        }
    }
    
    private void massageProviderProps(ModelNode provider) {
        if (!provider.hasDefined("properties")) return;
        ModelNode providerProps = provider.remove("properties");
        for (Property prop : providerProps.asPropertyList()) {
            ModelNode value = prop.getValue();
            if (isArray(value.asString().trim())) {
                provider.get(prop.getName()).set(ModelNode.fromString(value.asString()).asList());
            } else {
                provider.get(prop.getName()).set(value);
            }
        }
    }
    
    private boolean isArray(String value) {
        return value.startsWith("[") && value.endsWith("]");
    }
    
    void setWebContext(String webContext) {
        this.webContext = webContext;
    }

    String getWebContext() {
        return webContext;
    }

    boolean isKeycloakServerDeployment(String deploymentName) {
        return DEPLOYMENT_NAME.equals(deploymentName);
    }
}
