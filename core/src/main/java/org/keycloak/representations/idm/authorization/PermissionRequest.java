/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionRequest {

    private String resourceId;
    private Set<String> scopes;
    private String resourceServerId;

    public PermissionRequest(String resourceId, String... scopes) {
        this.resourceId = resourceId;
        if (scopes != null) {
            this.scopes = new HashSet(Arrays.asList(scopes));
        }
    }

    public PermissionRequest() {
        this(null, null);
    }

    public String getResourceId() {
        return resourceId;
    }

    @JsonProperty("resource_id")
    public void setResourceId(String resourceSetId) {
        this.resourceId = resourceSetId;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    @JsonProperty("resource_scopes")
    public void setScopes(Set<String> scopes) {
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
