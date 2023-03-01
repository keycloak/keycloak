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

package org.keycloak.models.map.storage.hotRod.clientscope;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodProtocolMapperEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodAttributeEntityNonIndexed;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.clientscope.MapClientScopeEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.clientscope.HotRodClientScopeEntity.AbstractHotRodClientScopeEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.ClientScopeModel"
)
@Indexed
@ProtoDoc("schema-version: " + HotRodClientScopeEntity.VERSION)
public class HotRodClientScopeEntity extends AbstractHotRodEntity  {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodClientScopeEntity.class
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE,
            dependsOn = {CommonPrimitivesProtoSchemaInitializer.class, HotRodClientEntity.HotRodClientEntitySchema.class}
    )
    public interface HotRodClientScopeEntitySchema extends GeneratedSchema {
        HotRodClientScopeEntitySchema INSTANCE = new HotRodClientScopeEntitySchemaImpl();
    }


    @Basic(projectable = true)
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @ProtoField(number = 2)
    public String id;

    @Basic(sortable = true)
    @ProtoField(number = 3)
    public String realmId;

    @Basic(sortable = true)
    @ProtoField(number = 4)
    public String name;

    @ProtoField(number = 5)
    public String protocol;

    @ProtoField(number = 6)
    public String description;

    @ProtoField(number = 7)
    public Collection<String> scopeMappings;

    @ProtoField(number = 8)
    public Set<HotRodProtocolMapperEntity> protocolMappers;

    @ProtoField(number = 9)
    public Set<HotRodAttributeEntityNonIndexed> attributes;

    public static abstract class AbstractHotRodClientScopeEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodClientScopeEntity> implements MapClientScopeEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodClientScopeEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodClientScopeEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodClientScopeEntityDelegate.entityHashCode(this);
    }
}
