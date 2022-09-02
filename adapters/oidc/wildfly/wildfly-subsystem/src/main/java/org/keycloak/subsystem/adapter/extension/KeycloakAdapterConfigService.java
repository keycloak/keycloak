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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * This service keeps track of the entire Keycloak management model so as to provide
 * adapter configuration to each deployment at deploy time.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public final class KeycloakAdapterConfigService {

    private static final String CREDENTIALS_JSON_NAME = "credentials";
    
    private static final String REDIRECT_REWRITE_RULE_JSON_NAME = "redirect-rewrite-rules";

    private static final KeycloakAdapterConfigService INSTANCE = new KeycloakAdapterConfigService();

    public static KeycloakAdapterConfigService getInstance() {
        return INSTANCE;
    }

    private final Map<String, ModelNode> realms = new HashMap<String, ModelNode>();

    // keycloak-secured deployments
    private final Map<String, ModelNode> secureDeployments = new HashMap<String, ModelNode>();
    private final Set<String> elytronEnabledDeployments = new HashSet<>();


    private KeycloakAdapterConfigService() {
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

    public void addSecureDeployment(ModelNode operation, ModelNode model, boolean elytronEnabled) {
        ModelNode deployment = model.clone();
        String name = deploymentNameFromOp(operation);
        this.secureDeployments.put(name, deployment);
        if (elytronEnabled) {
            elytronEnabledDeployments.add(name);
        }
    }

    public void updateSecureDeployment(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        deployment.get(attrName).set(resolvedValue);
    }

    public void removeSecureDeployment(ModelNode operation) {
        String name = deploymentNameFromOp(operation);
        this.secureDeployments.remove(name);
        elytronEnabledDeployments.remove(name);
    }

    public void addCredential(ModelNode operation, ModelNode model) {
        ModelNode credentials = credentialsFromOp(operation);
        if (!credentials.isDefined()) {
            credentials = new ModelNode();
        }

        String credentialName = credentialNameFromOp(operation);
        if (!credentialName.contains(".")) {
            credentials.get(credentialName).set(model.get("value").asString());
        } else {
            String[] parts = credentialName.split("\\.");
            String provider = parts[0];
            String property = parts[1];
            ModelNode credential = credentials.get(provider);
            if (!credential.isDefined()) {
                credential = new ModelNode();
            }
            credential.get(property).set(model.get("value").asString());
            credentials.set(provider, credential);
        }

        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
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
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        return deployment.get(CREDENTIALS_JSON_NAME);
    }
    
     public void addRedirectRewriteRule(ModelNode operation, ModelNode model) {
        ModelNode redirectRewritesRules = redirectRewriteRuleFromOp(operation);
        if (!redirectRewritesRules.isDefined()) {
            redirectRewritesRules = new ModelNode();
        }
        String redirectRewriteRuleName = redirectRewriteRule(operation);
        redirectRewritesRules.get(redirectRewriteRuleName).set(model.get("value").asString());

        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        deployment.get(REDIRECT_REWRITE_RULE_JSON_NAME).set(redirectRewritesRules);
    }

    public void removeRedirectRewriteRule(ModelNode operation) {
        ModelNode redirectRewritesRules = redirectRewriteRuleFromOp(operation);
        if (!redirectRewritesRules.isDefined()) {
            throw new RuntimeException("Can not remove redirect rewrite rule.  No rules defined for deployment in op " + operation.toString());
        }

        String ruleName = credentialNameFromOp(operation);
        redirectRewritesRules.remove(ruleName);
    }

    public void updateRedirectRewriteRule(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode redirectRewritesRules = redirectRewriteRuleFromOp(operation);
        if (!redirectRewritesRules.isDefined()) {
            throw new RuntimeException("Can not update redirect rewrite rule.  No rules defined for deployment in op " + operation.toString());
        }

        String ruleName = credentialNameFromOp(operation);
        redirectRewritesRules.get(ruleName).set(resolvedValue);
    }

    private ModelNode redirectRewriteRuleFromOp(ModelNode operation) {
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        return deployment.get(REDIRECT_REWRITE_RULE_JSON_NAME);
    }

    private String realmNameFromOp(ModelNode operation) {
        return valueFromOpAddress(RealmDefinition.TAG_NAME, operation);
    }

    private String deploymentNameFromOp(ModelNode operation) {
        String deploymentName = valueFromOpAddress(SecureDeploymentDefinition.TAG_NAME, operation);

        if (deploymentName == null) {
            deploymentName = valueFromOpAddress(KeycloakHttpServerAuthenticationMechanismFactoryDefinition.TAG_NAME, operation);
        }

        if (deploymentName == null) {
            deploymentName = valueFromOpAddress(SecureServerDefinition.TAG_NAME, operation);
        }

        if (deploymentName == null) throw new RuntimeException("Can't find deployment name in address " + operation);

        return deploymentName;
    }

    private String credentialNameFromOp(ModelNode operation) {
        return valueFromOpAddress(CredentialDefinition.TAG_NAME, operation);
    }
    
    private String redirectRewriteRule(ModelNode operation) {
        return valueFromOpAddress(RedirecRewritetRuleDefinition.TAG_NAME, operation);
    }

    private String valueFromOpAddress(String addrElement, ModelNode operation) {
        return getValueOfAddrElement(operation.get(ADDRESS), addrElement);
    }

    private String getValueOfAddrElement(ModelNode address, String elementName) {
        for (ModelNode element : address.asList()) {
            if (element.has(elementName)) return element.get(elementName).asString();
        }

        return null;
    }

    public String getRealmName(DeploymentUnit deploymentUnit) {
        ModelNode deployment = getSecureDeployment(deploymentUnit);
        return deployment.get(RealmDefinition.TAG_NAME).asString();

    }

    protected boolean isDeploymentConfigured(DeploymentUnit deploymentUnit) {
        ModelNode deployment = getSecureDeployment(deploymentUnit);
        if (! deployment.isDefined()) {
            return false;
        }
        ModelNode resource = deployment.get(SecureDeploymentDefinition.RESOURCE.getName());
        return resource.isDefined();
    }

    public String getJSON(DeploymentUnit deploymentUnit) {
        ModelNode deployment = getSecureDeployment(deploymentUnit);
        String realmName = deployment.get(RealmDefinition.TAG_NAME).asString();
        ModelNode realm = this.realms.get(realmName);

        ModelNode json = new ModelNode();
        json.get(RealmDefinition.TAG_NAME).set(realmName);

        // Realm values set first.  Some can be overridden by deployment values.
        if (realm != null) setJSONValues(json, realm);
        setJSONValues(json, deployment);
        return json.toJSONString(true);
    }

    public String getJSON(String deploymentName) {
        ModelNode deployment = this.secureDeployments.get(deploymentName);
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
        synchronized (values) {
            for (Property prop : new ArrayList<>(values.asPropertyList())) {
                String name = prop.getName();
                ModelNode value = prop.getValue();
                if (value.isDefined()) {
                    json.get(name).set(value);
                }
            }
        }
    }

    public boolean isSecureDeployment(DeploymentUnit deploymentUnit) {
        //log.info("********* CHECK KEYCLOAK DEPLOYMENT: deployments.size()" + deployments.size());

        String deploymentName = preferredDeploymentName(deploymentUnit);
        return this.secureDeployments.containsKey(deploymentName);
    }

    public boolean isElytronEnabled(DeploymentUnit deploymentUnit) {
        return elytronEnabledDeployments.contains(preferredDeploymentName(deploymentUnit));
    }

    private ModelNode getSecureDeployment(DeploymentUnit deploymentUnit) {
        String deploymentName = preferredDeploymentName(deploymentUnit);
        return this.secureDeployments.containsKey(deploymentName)
          ? this.secureDeployments.get(deploymentName)
          : new ModelNode();
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
