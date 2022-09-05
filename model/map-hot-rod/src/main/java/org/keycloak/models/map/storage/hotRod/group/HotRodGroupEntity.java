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

package org.keycloak.models.map.storage.hotRod.group;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodAttributeEntityNonIndexed;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

import java.util.Objects;
import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.group.MapGroupEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.group.HotRodGroupEntity.AbstractHotRodGroupEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.GroupModel"
)
@ProtoDoc("@Indexed")
@ProtoDoc("schema-version: " + HotRodGroupEntity.VERSION)
public class HotRodGroupEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodGroupEntity.class
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE,
            dependsOn = {CommonPrimitivesProtoSchemaInitializer.class}
    )
    public interface HotRodGroupEntitySchema extends GeneratedSchema {
        HotRodGroupEntitySchema INSTANCE = new HotRodGroupEntitySchemaImpl();
    }

    public static abstract class AbstractHotRodGroupEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodGroupEntity> implements MapGroupEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodGroupEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }

        @Override
        public void setName(String name) {
            HotRodGroupEntity entity = getHotRodEntity();
            entity.updated |= ! Objects.equals(entity.name, name);
            entity.name = name;
            entity.nameLowercase = name == null ? null : name.toLowerCase();
        }
    }

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 2)
    public String id;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String realmId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 4)
    public String name;

    /**
     * Lowercase interpretation of {@link #name} field. Infinispan doesn't support case-insensitive LIKE for non-analyzed fields.
     * Search on analyzed fields can be case-insensitive (based on used analyzer) but doesn't support ORDER BY analyzed field.
     */
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 5)
    public String nameLowercase;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 6)
    public String parentId;

    @ProtoField(number = 7)
    public Set<HotRodAttributeEntityNonIndexed> attributes;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 8)
    public Set<String> grantedRoles;

    @Override
    public boolean equals(Object o) {
        return HotRodGroupEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodGroupEntityDelegate.entityHashCode(this);
    }
}
