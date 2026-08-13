/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.rest.representation;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.infinispan.client.hotrod.ServerStatistics;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheStats {

    @JsonProperty(ServerStatistics.STORES)
    private Integer stores;

    @JsonProperty("globalStores")
    private Integer globalStores;

    private Map<String, String> otherStats = new HashMap<>();


    public Integer getStores() {
        return stores;
    }

    public void setStores(Integer stores) {
        this.stores = stores;
    }

    public Integer getGlobalStores() {
        return globalStores;
    }

    public void setGlobalStores(Integer globalStores) {
        this.globalStores = globalStores;
    }

    @JsonAnyGetter
    public Map<String, String> getOtherStats() {
        return otherStats;
    }

    @JsonAnySetter
    public void setOtherStats(String name, String value) {
        otherStats.put(name, value);
    }
}
