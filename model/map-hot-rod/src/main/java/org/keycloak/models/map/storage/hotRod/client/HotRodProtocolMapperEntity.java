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

package org.keycloak.models.map.storage.hotRod.client;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;

import java.util.Objects;
import java.util.Set;

@GenerateHotRodEntityImplementation(implementInterface = "org.keycloak.models.map.client.MapProtocolMapperEntity")
public class HotRodProtocolMapperEntity extends AbstractHotRodEntity {
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
    public Set<HotRodPair<String, String>> config;

    @Override
    public boolean equals(Object o) {
        return HotRodProtocolMapperEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodProtocolMapperEntityDelegate.entityHashCode(this);
    }
}
