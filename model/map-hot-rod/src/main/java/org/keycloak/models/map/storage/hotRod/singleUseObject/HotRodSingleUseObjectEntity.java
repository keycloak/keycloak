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

package org.keycloak.models.map.storage.hotRod.singleUseObject;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.singleUseObject.HotRodSingleUseObjectEntity.AbstractHotRodSingleUseObjectEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.ActionTokenValueModel"
)
@ProtoDoc("@Indexed")
@ProtoDoc("schema-version: " + HotRodSingleUseObjectEntity.VERSION)
public class HotRodSingleUseObjectEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodSingleUseObjectEntity.class
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE,
            dependsOn = {CommonPrimitivesProtoSchemaInitializer.class}
    )
    public interface HotRodSingleUseObjectEntitySchema extends GeneratedSchema {
        HotRodSingleUseObjectEntitySchema INSTANCE = new HotRodSingleUseObjectEntitySchemaImpl();
    }

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @ProtoField(number = 2)
    public String id;

    @ProtoField(number = 3)
    public String objectKey;

    @ProtoField(number = 4)
    public String userId;

    @ProtoField(number = 5)
    public String actionId;

    @ProtoField(number = 6)
    public String actionVerificationNonce;

    @ProtoField(number = 7)
    public Long expiration;

    @ProtoField(number = 8)
    public Set<HotRodPair<String, String>> notes;

    public static abstract class AbstractHotRodSingleUseObjectEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodSingleUseObjectEntity> implements MapSingleUseObjectEntity {

        @Override
        public String getId() {
            HotRodSingleUseObjectEntity hotRodEntity = getHotRodEntity();
            return hotRodEntity != null ? hotRodEntity.id : null;
        }

        @Override
        public void setId(String id) {
            HotRodSingleUseObjectEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodSingleUseObjectEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodSingleUseObjectEntityDelegate.entityHashCode(this);
    }
}
