/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.representations.idm.authorization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEvaluationRequest {

    private Map<String, Map<String, String>> context = new HashMap<>();
    private List<ResourceRepresentation> resources = new LinkedList<>();
    private String clientId;
    private String userId;
    private List<String> roleIds = new LinkedList<>();
    private boolean entitlements;

    public Map<String, Map<String, String>> getContext() {
        return this.context;
    }

    public void setContext(Map<String, Map<String, String>> context) {
        this.context = context;
    }

    public List<ResourceRepresentation> getResources() {
        return this.resources;
    }

    public void setResources(List<ResourceRepresentation> resources) {
        this.resources = resources;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getRoleIds() {
        return this.roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    public boolean isEntitlements() {
        return entitlements;
    }

    public void setEntitlements(boolean entitlements) {
        this.entitlements = entitlements;
    }

    public PolicyEvaluationRequest addResource(String name, String... scopes) {
        if (resources == null) {
            resources = new LinkedList<>();
        }
        resources.add(new ResourceRepresentation(name, scopes));
        return this;
    }


}
