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

package org.keycloak.models.map.client;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.common.HotRodPair;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class HotRodProtocolMapperEntity implements MapProtocolMapperEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public String name;
    @ProtoField(number = 3)
    public String protocol;
    @ProtoField(number = 4)
    public String protocolMapper;
//    @ProtoField(number = 5, defaultValue = "false")
//    public boolean consentRequired;
//    @ProtoField(number = 5)
//    public String consentText;
    @ProtoField(number = 5)
    public Set<HotRodPair<String, String>> config = new LinkedHashSet<>();
    
    private boolean updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HotRodProtocolMapperEntity entity = (HotRodProtocolMapperEntity) o;

        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        updated |= !Objects.equals(this.id, id);
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        updated |= !Objects.equals(this.name, name);
        this.name = name;
    }

    @Override
    public String getProtocolMapper() {
        return protocolMapper;
    }

    @Override
    public void setProtocolMapper(String protocolMapper) {
        updated |= !Objects.equals(this.protocolMapper, protocolMapper);
        this.protocolMapper = protocolMapper;
    }

    @Override
    public Map<String, String> getConfig() {
        return config.stream().collect(Collectors.toMap(HotRodPair::getFirst, HotRodPair::getSecond));
    }

    @Override
    public void setConfig(Map<String, String> config) {
        updated |= !Objects.equals(this.config, config);
        this.config.clear();

        config.entrySet().stream().map(entry -> new HotRodPair<>(entry.getKey(), entry.getValue())).forEach(this.config::add);
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }
}
