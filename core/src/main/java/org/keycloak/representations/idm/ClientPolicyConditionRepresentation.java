/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Objects;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientPolicyConditionRepresentation {

    @JsonProperty("condition")
    private String conditionProviderId;

    @JsonProperty("configuration")
    @Schema(type= SchemaType.OBJECT,
            description = "Configuration settings as a JSON object",
            additionalProperties = Schema.True.class)
    private JsonNode configuration;

    public String getConditionProviderId() {
        return conditionProviderId;
    }

    public void setConditionProviderId(String conditionProviderId) {
        this.conditionProviderId = conditionProviderId;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientPolicyConditionRepresentation that = (ClientPolicyConditionRepresentation) o;
        return Objects.equals(conditionProviderId, that.conditionProviderId) && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionProviderId, configuration);
    }
}
