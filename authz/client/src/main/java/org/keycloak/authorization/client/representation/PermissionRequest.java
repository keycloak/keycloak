/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.authorization.client.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionRequest {

    private String resourceSetId;
    private Set<String> scopes;

    public PermissionRequest() {

    }

    public PermissionRequest(String resourceSetId, Set<String> scopes) {
        this.resourceSetId = resourceSetId;
        this.scopes = scopes;
    }

    public PermissionRequest(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public String getResourceSetId() {
        return this.resourceSetId;
    }

    /**
     * @deprecated UMA 1.0. Remove once we move to UMA 2.0.
     */
    @JsonProperty("resource_set_id")
    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    /**
        * @deprecated UMA 1.0. Remove once we move to UMA 2.0.
    */
    @JsonProperty("resource_set_name")
    public void setResourceSetName(String resourceSetName) {
        this.resourceSetId = resourceSetName;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }

    /**
     * @deprecated UMA 1.0. Remove once we move to UMA 2.0.
     */
    @JsonProperty("scopes")
    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty("resource_scopes")
    public void setResourceScopes(Set<String> scopes) {
        this.scopes = scopes;
    }
}
