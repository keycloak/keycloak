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

    @JsonProperty("resource_set_id")
    private final String resourceSetId;

    @JsonProperty("resource_set_name")
    private final String resourceSetName;

    private final Set<String> scopes;

    public PermissionRequest(String resourceSetId, String... scopes) {
        this.resourceSetId = resourceSetId;

        if (scopes != null) {
            this.scopes = new HashSet(Arrays.asList(scopes));
        } else {
            this.scopes = new HashSet<>();
        }

        this.resourceSetName = null;
    }

    public PermissionRequest() {
        this(null, null);
    }

    public String getResourceSetId() {
        return this.resourceSetId;
    }

    public String getResourceSetName() {
        return resourceSetName;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }
}
