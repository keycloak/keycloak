/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.keycloak.models.ProtocolMapperModel;

public class MapProtocolMapperUtils {

    private final String protocol;
    private static final ConcurrentMap<String, MapProtocolMapperUtils> INSTANCES = new ConcurrentHashMap<>();

    private MapProtocolMapperUtils(String protocol) {
        this.protocol = protocol;
    }

    public static MapProtocolMapperUtils instanceFor(String protocol) {
        Objects.requireNonNull(protocol);
        return INSTANCES.computeIfAbsent(protocol, MapProtocolMapperUtils::new);
    }

    public static MapProtocolMapperEntity fromModel(ProtocolMapperModel model) {
        MapProtocolMapperEntity res = new MapProtocolMapperEntityImpl();
        res.setId(model.getId());
        res.setName(model.getName());
        res.setProtocolMapper(model.getProtocolMapper());
        res.setConfig(model.getConfig());
        return res;
    }

    public ProtocolMapperModel toModel(MapProtocolMapperEntity entity) {
        ProtocolMapperModel res = new ProtocolMapperModel();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setProtocolMapper(entity.getProtocolMapper());
        Map<String, String> config = entity.getConfig();
        res.setConfig(config == null ? new HashMap<>(): new HashMap<>(config));
        res.setProtocol(protocol);
        return res;
    }

}
