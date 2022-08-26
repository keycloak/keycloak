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

package org.keycloak.models.map.storage.hotRod.loginFailure;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.loginFailure.HotRodUserLoginFailureEntity.AbstractHotRodUserLoginFailureEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.UserLoginFailureModel"
)
@ProtoDoc("@Indexed")
@ProtoDoc("schema-version: " + HotRodUserLoginFailureEntity.VERSION)
public class HotRodUserLoginFailureEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodUserLoginFailureEntity.class
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE)
    public interface HotRodUserLoginFailureEntitySchema extends GeneratedSchema {
        HotRodUserLoginFailureEntitySchema INSTANCE = new HotRodUserLoginFailureEntitySchemaImpl();
    }

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @ProtoField(number = 2)
    public String id;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String realmId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 4)
    public String userId;

    @ProtoField(number = 5)
    public Long failedLoginNotBefore;

    @ProtoField(number = 6)
    public Integer numFailures;

    @ProtoField(number = 7)
    public Long lastFailure;

    @ProtoField(number = 8)
    public String lastIPFailure;

    public static abstract class AbstractHotRodUserLoginFailureEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodUserLoginFailureEntity> implements MapUserLoginFailureEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodUserLoginFailureEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }

        @Override
        public void clearFailures() {
            HotRodUserLoginFailureEntity entity = getHotRodEntity();
            entity.updated |= getFailedLoginNotBefore() != null || getNumFailures() != null || getLastFailure() != null || getLastIPFailure() != null;
            setFailedLoginNotBefore(null);
            setNumFailures(null);
            setLastFailure(null);
            setLastIPFailure(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodUserLoginFailureEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodUserLoginFailureEntityDelegate.entityHashCode(this);
    }
}
