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

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientPolicyConditionRepresentation {

    @JsonProperty("condition")
    private String conditionProviderId;

    private ClientPolicyConditionConfigurationRepresentation configuration;

    public ClientPolicyConditionRepresentation() {
    }

    public ClientPolicyConditionRepresentation(String conditionProviderId, ClientPolicyConditionConfigurationRepresentation configuration) {
        this.conditionProviderId = conditionProviderId;
        this.configuration = configuration;
    }

    public String getConditionProviderId() {
        return conditionProviderId;
    }

    public void setConditionProviderId(String conditionProviderId) {
        this.conditionProviderId = conditionProviderId;
    }

    public ClientPolicyConditionConfigurationRepresentation getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ClientPolicyConditionConfigurationRepresentation configuration) {
        this.configuration = configuration;
    }
}
