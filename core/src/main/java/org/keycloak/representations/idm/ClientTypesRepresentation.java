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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientTypesRepresentation {

    @JsonProperty("client-types")
    private List<ClientTypeRepresentation> realmClientTypes;

    @JsonProperty("global-client-types")
    private List<ClientTypeRepresentation> globalClientTypes;

    public ClientTypesRepresentation() {
    }

    public ClientTypesRepresentation(List<ClientTypeRepresentation> realmClientTypes, List<ClientTypeRepresentation> globalClientTypes) {
        this.realmClientTypes = realmClientTypes;
        this.globalClientTypes = globalClientTypes;
    }

    public List<ClientTypeRepresentation> getRealmClientTypes() {
        return realmClientTypes;
    }

    public void setRealmClientTypes(List<ClientTypeRepresentation> realmClientTypes) {
        this.realmClientTypes = realmClientTypes;
    }

    public List<ClientTypeRepresentation> getGlobalClientTypes() {
        return globalClientTypes;
    }

    public void setGlobalClientTypes(List<ClientTypeRepresentation> globalClientTypes) {
        this.globalClientTypes = globalClientTypes;
    }
}