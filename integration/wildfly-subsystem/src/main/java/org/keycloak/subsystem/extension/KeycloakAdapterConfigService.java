/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * This service keeps track of the entire Keycloak management model so as to provide
 * adapter configuration to each deployment at deploy time.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public final class KeycloakAdapterConfigService implements Service<KeycloakAdapterConfigService> {
    protected Logger log = Logger.getLogger(KeycloakAdapterConfigService.class);
    private static final String CREDENTIALS_JSON_NAME = "credentials";

    // Right now this is used as a service, but I'm not sure it really needs to be implemented that way.
    // It's also a singleton serving the entire subsystem, but the INSTANCE variable is currently only
    // used during initialization of the subsystem.
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("KeycloakAdapterConfigService");
    public static final KeycloakAdapterConfigService INSTANCE = new KeycloakAdapterConfigService();

    private Map<String, ModelNode> realms = new HashMap<String, ModelNode>();
    private Map<String, ModelNode> deployments = new HashMap<String, ModelNode>();

    private KeycloakAdapterConfigService() {

    }

    @Override
    public void start(StartContext sc) throws StartException {

    }

    @Override
    public void stop(StopContext sc) {

    }

    @Override
    public KeycloakAdapterConfigService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void addRealm(ModelNode operation, ModelNode model) {
        this.realms.put(realmNameFromOp(operation), model.clone());
    }

    public void updateRealm(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode realm = this.realms.get(realmNameFromOp(operation));
        realm.get(attrName).set(resolvedValue);
    }

    public void removeRealm(ModelNode operation) {
        this.realms.remove(realmNameFromOp(operation));
    }

    public void addSecureDeployment(ModelNode operation, ModelNode model) {
        ModelNode deployment = model.clone();
        this.deployments.put(deploymentNameFromOp(operation), deployment);
    }

    public void updateSecureDeployment(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode deployment = this.deployments.get(deploymentNameFromOp(operation));
        deployment.get(attrName).set(resolvedValue);
    }

    public void removeSecureDeployment(ModelNode operation) {
        this.deployments.remove(deploymentNameFromOp(operation));
    }

    public void addCredential(ModelNode operation, ModelNode model) {
        ModelNode credentials = credentialsFromOp(operation);
        if (!credentials.isDefined()) {
            credentials = new ModelNode();
        }

        String credentialName = credentialNameFromOp(operation);
        credentials.get(credentialName).set(model.get("value").asString());

        ModelNode deployment = this.deployments.get(deploymentNameFromOp(operation));
        deployment.get(CREDENTIALS_JSON_NAME).set(credentials);
    }

    public void removeCredential(ModelNode operation) {
        ModelNode credentials = credentialsFromOp(operation);
        if (!credentials.isDefined()) {
            throw new RuntimeException("Can not remove credential.  No credential defined for deployment in op " + operation.toString());
        }

        String credentialName = credentialNameFromOp(operation);
        credentials.remove(credentialName);
    }

    public void updateCredential(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode credentials = credentialsFromOp(operation);
        if (!credentials.isDefined()) {
            throw new RuntimeException("Can not update credential.  No credential defined for deployment in op " + operation.toString());
        }

        String credentialName = credentialNameFromOp(operation);
        credentials.get(credentialName).set(resolvedValue);
    }

    private ModelNode credentialsFromOp(ModelNode operation) {
        ModelNode deployment = this.deployments.get(deploymentNameFromOp(operation));
        return deployment.get(CREDENTIALS_JSON_NAME);
    }

    private String realmNameFromOp(ModelNode operation) {
        return valueFromOpAddress(RealmDefinition.TAG_NAME, operation);
    }

    private String deploymentNameFromOp(ModelNode operation) {
        return valueFromOpAddress(SecureDeploymentDefinition.TAG_NAME, operation);
    }

    private String credentialNameFromOp(ModelNode operation) {
        return valueFromOpAddress(CredentialDefinition.TAG_NAME, operation);
    }

    private String valueFromOpAddress(String addrElement, ModelNode operation) {
        String deploymentName = getValueOfAddrElement(operation.get(ADDRESS), addrElement);
        if (deploymentName == null) throw new RuntimeException("Can't find '" + addrElement + "' in address " + operation.toString());
        return deploymentName;
    }

    private String getValueOfAddrElement(ModelNode address, String elementName) {
        for (ModelNode element : address.asList()) {
            if (element.has(elementName)) return element.get(elementName).asString();
        }

        return null;
    }

    public String getRealmName(String deploymentName) {
        ModelNode deployment = this.deployments.get(deploymentName);
        return deployment.get(RealmDefinition.TAG_NAME).asString();

    }

    public String getJSON(String deploymentName) {
        ModelNode deployment = this.deployments.get(deploymentName);
        String realmName = deployment.get(RealmDefinition.TAG_NAME).asString();
        ModelNode realm = this.realms.get(realmName);

        ModelNode json = new ModelNode();
        json.get(RealmDefinition.TAG_NAME).set(realmName);

        // Realm values set first.  Some can be overridden by deployment values.
        if (realm != null) setJSONValues(json, realm);
        setJSONValues(json, deployment);
        return json.toJSONString(true);
    }

    private void setJSONValues(ModelNode json, ModelNode values) {
        for (Property prop : values.asPropertyList()) {
            String name = prop.getName();
            ModelNode value = prop.getValue();
            if (value.isDefined()) {
                json.get(name).set(value);
            }
        }
    }

    public boolean isKeycloakDeployment(String deploymentName) {
        //log.info("********* CHECK KEYCLOAK DEPLOYMENT: deployments.size()" + deployments.size());

        return this.deployments.containsKey(deploymentName);
    }

    static KeycloakAdapterConfigService find(ServiceRegistry registry) {
        ServiceController<?> container = registry.getService(KeycloakAdapterConfigService.SERVICE_NAME);
        if (container != null) {
            KeycloakAdapterConfigService service = (KeycloakAdapterConfigService)container.getValue();
            return service;
        }
        return null;
    }

    static KeycloakAdapterConfigService find(OperationContext context) {
        return find(context.getServiceRegistry(true));
    }
}
