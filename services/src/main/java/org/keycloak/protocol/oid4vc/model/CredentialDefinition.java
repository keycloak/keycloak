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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.keycloak.models.oid4vci.CredentialScopeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public static CredentialDefinition parse(CredentialScopeModel credentialModel) {
        List<String> contexts = Optional.of(credentialModel.getVcContexts())
                                        .filter(list -> !list.isEmpty())
                                        .orElseGet(() -> new ArrayList<>(List.of(credentialModel.getName())));
        List<String> types = Optional.ofNullable(credentialModel.getSupportedCredentialTypes())
                                     .filter(list -> !list.isEmpty())
                                     .orElseGet(() -> new ArrayList<>(List.of(credentialModel.getName())));

        return new CredentialDefinition().setContext(contexts)
                                         .setType(types);
    }

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
}
