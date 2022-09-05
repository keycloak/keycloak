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

package org.keycloak.models.map.storage.hotRod.authorization;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.authorization.HotRodPermissionTicketEntity.AbstractHotRodPermissionTicketEntity",
        topLevelEntity = true,
        modelClass = "org.keycloak.authorization.model.PermissionTicket",
        cacheName = "authz"
)
@ProtoDoc("@Indexed")
@ProtoDoc("schema-version: " + HotRodPermissionTicketEntity.VERSION)
public class HotRodPermissionTicketEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodPermissionTicketEntity.class,
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE)
    public interface HotRodPermissionTicketEntitySchema extends GeneratedSchema {
        HotRodPermissionTicketEntitySchema INSTANCE = new HotRodPermissionTicketEntitySchemaImpl();
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

    @ProtoField(number = 4)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public String owner;

    @ProtoField(number = 5)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public String requester;

    @ProtoField(number = 6)
    public Long createdTimestamp;

    @ProtoField(number = 7)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public Long grantedTimestamp;

    @ProtoField(number = 8)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public String resourceId;

    @ProtoField(number = 9)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public String scopeId;

    @ProtoField(number = 10)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public String resourceServerId;

    @ProtoField(number = 11)
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    public String policyId;

    public static abstract class AbstractHotRodPermissionTicketEntity extends UpdatableHotRodEntityDelegateImpl<HotRodPermissionTicketEntity> implements MapPermissionTicketEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodPermissionTicketEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodPermissionTicketEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodPermissionTicketEntityDelegate.entityHashCode(this);
    }
}