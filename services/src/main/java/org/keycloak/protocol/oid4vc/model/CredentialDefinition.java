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
package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pojo to represent a CredentialDefinition for internal handling
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialDefinition {

    @JsonProperty("@context")
    private List<String> context;
    private List<String> type = new ArrayList<>();
    private CredentialSubject credentialSubject = new CredentialSubject();

    public List<String> getContext() {
        return context;
    }

    public CredentialDefinition setContext(List<String> context) {
        this.context = context;
        return this;
    }

    public List<String> getType() {
        return type;
    }

    public CredentialDefinition setType(List<String> type) {
        this.type = type;
        return this;
    }

    public CredentialSubject getCredentialSubject() {
        return credentialSubject;
    }

    public CredentialDefinition setCredentialSubject(CredentialSubject credentialSubject) {
        this.credentialSubject = credentialSubject;
        return this;
    }

    public String toJsonString() {
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CredentialDefinition fromJsonString(String jsonString) {
        try {
            return JsonSerialization.readValue(jsonString, CredentialDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
