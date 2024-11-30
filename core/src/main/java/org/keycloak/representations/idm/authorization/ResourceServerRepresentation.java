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
package org.keycloak.representations.idm.authorization;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceServerRepresentation {

    private String id;

    private String clientId;
    private String name;
    private boolean allowRemoteResourceManagement = true;
    private PolicyEnforcementMode policyEnforcementMode = PolicyEnforcementMode.ENFORCING;
    private List<ResourceRepresentation> resources = emptyList();
    private List<PolicyRepresentation> policies = emptyList();
    private List<ScopeRepresentation> scopes = emptyList();
    private DecisionStrategy decisionStrategy;
    private AuthorizationSchema authorizationSchema;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAllowRemoteResourceManagement() {
        return this.allowRemoteResourceManagement;
    }

    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        this.allowRemoteResourceManagement = allowRemoteResourceManagement;
    }

    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return this.policyEnforcementMode;
    }

    public void setPolicyEnforcementMode(PolicyEnforcementMode policyEnforcementMode) {
        this.policyEnforcementMode = policyEnforcementMode;
    }

    public void setResources(List<ResourceRepresentation> resources) {
        this.resources = resources;
    }

    public List<ResourceRepresentation> getResources() {
        return resources;
    }

    public void setPolicies(List<PolicyRepresentation> policies) {
        this.policies = policies;
    }

    public List<PolicyRepresentation> getPolicies() {
        return policies;
    }

    public void setScopes(List<ScopeRepresentation> scopes) {
        this.scopes = scopes;
    }

    public List<ScopeRepresentation> getScopes() {
        return scopes;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    public DecisionStrategy getDecisionStrategy() {
        return decisionStrategy;
    }

    public void setAuthorizationSchema(AuthorizationSchema authorizationSchema) {
        this.authorizationSchema = authorizationSchema;
    }

    public AuthorizationSchema getAuthorizationSchema() {
        return authorizationSchema;
    }
}
