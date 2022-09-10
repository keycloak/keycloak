/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class ScriptProviderDescriptor {

    public static final String AUTHENTICATORS = "authenticators";
    public static final String POLICIES = "policies";
    public static final String MAPPERS = "mappers";

    public static final String SAML_MAPPERS = "saml-mappers";

    private Map<String, List<ScriptProviderMetadata>> providers = new HashMap<>();

    @JsonUnwrapped
    @JsonGetter
    public Map<String, List<ScriptProviderMetadata>> getProviders() {
        return providers;
    }

    @JsonSetter
    public void setAuthenticators(List<ScriptProviderMetadata> metadata) {
        providers.put(AUTHENTICATORS, metadata);
    }

    @JsonSetter
    public void setPolicies(List<ScriptProviderMetadata> metadata) {
        providers.put(POLICIES, metadata);
    }

    @JsonSetter
    public void setMappers(List<ScriptProviderMetadata> metadata) {
        providers.put(MAPPERS, metadata);
    }

    @JsonSetter(SAML_MAPPERS)
    public void setSAMLMappers(List<ScriptProviderMetadata> metadata) {
        providers.put(SAML_MAPPERS, metadata);
    }

    public void addAuthenticator(String name, String fileName) {
        addProvider(AUTHENTICATORS, name, fileName, null);
    }
    
    private void addProvider(String type, String name, String fileName, String description) {
        List<ScriptProviderMetadata> authenticators = providers.get(type);

        if (authenticators == null) {
            authenticators = new ArrayList<>();
            providers.put(type, authenticators);
        }

        authenticators.add(new ScriptProviderMetadata(name, fileName, description));
    }

    public void addPolicy(String name, String fileName) {
        addProvider(POLICIES, name, fileName, null);
    }

    public void addMapper(String name, String fileName) {
        addProvider(MAPPERS, name, fileName, null);
    }

    public void addSAMLMapper(String name, String fileName) {
        addProvider(SAML_MAPPERS, name, fileName, null);
    }
}
