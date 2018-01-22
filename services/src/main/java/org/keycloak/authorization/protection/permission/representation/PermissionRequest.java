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

package org.keycloak.authorization.protection.permission.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionRequest {

    private String resourceSetId;
    private Set<String> scopes;
    private String resourceServerId;

    public PermissionRequest(String resourceSetId, String... scopes) {
        this.resourceSetId = resourceSetId;
        if (scopes != null) {
            this.scopes = new HashSet(Arrays.asList(scopes));
        }
    }

    public PermissionRequest() {
        this(null, null);
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    @JsonProperty("resource_id")
    public void setResourceId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
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
        return scopes;
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

    @JsonProperty("resource_server_id")
    public void setResourceServerId(String resourceServerId) {
        this.resourceServerId = resourceServerId;
    }

    public String getResourceServerId() {
        return resourceServerId;
    }
}
