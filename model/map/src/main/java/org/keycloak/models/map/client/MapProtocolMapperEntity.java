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

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
@GenerateEntityImplementations
@DeepCloner.Root
public interface MapProtocolMapperEntity extends UpdatableEntity, AbstractEntity {

    String getName();
    void setName(String name);

    String getProtocolMapper();
    void setProtocolMapper(String protocolMapper);

    Map<String, String> getConfig();
    void setConfig(Map<String, String> config);
}
