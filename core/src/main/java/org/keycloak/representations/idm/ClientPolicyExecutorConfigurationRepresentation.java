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

/**
 * Just adds some type-safety to the ClientPolicyExecutorConfiguration
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientPolicyExecutorConfigurationRepresentation {

    private Map<String, Object> configAsMap = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getConfigAsMap() {
        return configAsMap;
    }

    @JsonAnySetter
    public void setConfigAsMap(String name, Object value) {
        this.configAsMap.put(name, value);
    }

    public boolean validateConfig(){
        return  true;
    }
}
