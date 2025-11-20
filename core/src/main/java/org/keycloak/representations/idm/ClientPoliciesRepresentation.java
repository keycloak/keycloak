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
 */

package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Client Policies' (the set of all Client Policy) external representation class
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientPoliciesRepresentation {
    protected List<ClientPolicyRepresentation> policies = new ArrayList<>();
    private List<ClientPolicyRepresentation> globalPolicies;

    public List<ClientPolicyRepresentation> getPolicies() {
        return policies;
    }

    public void setPolicies(List<ClientPolicyRepresentation> policies) {
        this.policies = policies;
    }

    public List<ClientPolicyRepresentation> getGlobalPolicies() {
        return globalPolicies;
    }

    public void setGlobalPolicies(List<ClientPolicyRepresentation> globalPolicies) {
        this.globalPolicies = globalPolicies;
    }

    @Override
    public int hashCode() {
        return JsonSerialization.mapper.convertValue(this, JsonNode.class).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientPoliciesRepresentation)) return false;
        JsonNode jsonNode = JsonSerialization.mapper.convertValue(this, JsonNode.class);
        JsonNode jsonNodeThat = JsonSerialization.mapper.convertValue(obj, JsonNode.class);
        return jsonNode.equals(jsonNodeThat);
    }

}
