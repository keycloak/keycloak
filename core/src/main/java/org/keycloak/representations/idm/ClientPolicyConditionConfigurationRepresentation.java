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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Just adds some type-safety to the ClientPolicyConditionConfiguration
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientPolicyConditionConfigurationRepresentation {

    private Map<String, Object> configAsMap = new HashMap<>();

    @JsonProperty("is-negative-logic")
    private Boolean negativeLogic;

    public Boolean isNegativeLogic() {
        return negativeLogic;
    }

    public void setNegativeLogic(Boolean negativeLogic) {
        this.negativeLogic = negativeLogic;
    }

    @JsonAnyGetter
    public Map<String, Object> getConfigAsMap() {
        return configAsMap;
    }

    @JsonAnySetter
    public void setConfigAsMap(String name, Object value) {
        this.configAsMap.put(name, value);
    }
}
