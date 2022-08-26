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

package org.keycloak.models.map.storage.hotRod.userSession;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceServerEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodStringPair;
import org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntityDelegate;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.userSession.MapUserSessionEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionEntity.AbstractHotRodUserSessionEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.UserSessionModel"
)
@ProtoDoc("@Indexed")
@ProtoDoc("schema-version: " + HotRodResourceServerEntity.VERSION)
public class HotRodUserSessionEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodUserSessionEntity.class,
                    HotRodAuthenticatedClientSessionEntityReference.class,
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE,
            dependsOn = {CommonPrimitivesProtoSchemaInitializer.class}
    )
    public interface HotRodUserSessionEntitySchema extends GeneratedSchema {
        HotRodUserSessionEntitySchema INSTANCE = new HotRodUserSessionEntitySchemaImpl();
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

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 5)
    public String brokerSessionId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 6)
    public String brokerUserId;

    @ProtoField(number = 7)
    public String loginUsername;

    @ProtoField(number = 8)
    public String ipAddress;

    @ProtoField(number = 9)
    public String authMethod;

    @ProtoField(number = 10)
    public Boolean rememberMe;

    @ProtoField(number = 11)
    public Long timestamp;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 12)
    public Long lastSessionRefresh;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 13)
    public Long expiration;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 14)
    public Set<HotRodStringPair> notes;

    @ProtoField(number = 15)
    public Integer state;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 16)
    public Set<HotRodAuthenticatedClientSessionEntityReference> authenticatedClientSessions;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 17)
    public Boolean offline;

    public static abstract class AbstractHotRodUserSessionEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodUserSessionEntity> implements MapUserSessionEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodUserSessionEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }

        @Override
        public UserSessionModel.SessionPersistenceState getPersistenceState() {
            return UserSessionModel.SessionPersistenceState.PERSISTENT;
        }

        @Override
        public void setPersistenceState(UserSessionModel.SessionPersistenceState persistenceState) {
            if (persistenceState != null && UserSessionModel.SessionPersistenceState.PERSISTENT != persistenceState) {
                throw new IllegalArgumentException("Transient session should not be stored in the HotRod.");
            }
        }

        @Override
        public boolean isUpdated() {
            return getHotRodEntity().updated
                    || Optional.ofNullable(getAuthenticatedClientSessions()).orElseGet(Collections::emptySet).stream().anyMatch(UpdatableEntity::isUpdated);
        }

        @Override
        public void clearUpdatedFlag() {
            getHotRodEntity().updated = false;
            Optional.ofNullable(getAuthenticatedClientSessions()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
        }

        @Override
        public Optional<MapAuthenticatedClientSessionEntity> getAuthenticatedClientSession(String clientUUID) {
            Set<HotRodAuthenticatedClientSessionEntityReference> acss = getHotRodEntity().authenticatedClientSessions;
            if (acss == null || acss.isEmpty()) return Optional.empty();

            return acss.stream()
                    .filter(acs -> Objects.equals(acs.clientId, clientUUID))
                    .findFirst()
                    .map(HotRodTypesUtils::migrateHotRodAuthenticatedClientSessionEntityReferenceToMapAuthenticatedClientSessionEntity);
        }

        @Override
        public Boolean removeAuthenticatedClientSession(String clientUUID) {
            Set<HotRodAuthenticatedClientSessionEntityReference> acss = getHotRodEntity().authenticatedClientSessions;
            boolean removed = acss != null && acss.removeIf(uc -> Objects.equals(uc.clientId, clientUUID));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public void clearAuthenticatedClientSessions() {
            HotRodUserSessionEntity entity = getHotRodEntity();
            entity.updated = entity.authenticatedClientSessions != null;
            entity.authenticatedClientSessions = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodUserSessionEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodUserSessionEntityDelegate.entityHashCode(this);
    }
}
