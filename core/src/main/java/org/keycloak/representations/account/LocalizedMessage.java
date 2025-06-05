/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author rmartinc
 */
public class LocalizedMessage {

    private final String key;
    private final String[] parameters;

    @JsonCreator
    public LocalizedMessage(@JsonProperty("key") String key, @JsonProperty("parameters") String... parameters) {
        this.key = key;
        this.parameters = parameters == null || parameters.length == 0? null : parameters;
    }

    public String getKey() {
        return key;
    }

    public String[] getParameters() {
        return parameters;
    }
}
