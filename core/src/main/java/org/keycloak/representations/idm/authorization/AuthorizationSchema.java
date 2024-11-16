/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorizationSchema {

    private final Set<ResourceType> resourceTypes;

    @JsonCreator
    public AuthorizationSchema(@JsonProperty("resourceTypes") ResourceType... resourceTypes) {
        this.resourceTypes = Arrays.stream(resourceTypes).collect(Collectors.toSet());
    }

    public Set<ResourceType> getResourceTypes() {
        return Collections.unmodifiableSet(resourceTypes);
    }
}
