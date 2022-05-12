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

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodStringPair;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;
import org.keycloak.models.map.userSession.MapUserSessionEntity;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.userSession.MapUserSessionEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionEntity.AbstractHotRodUserSessionEntityDelegate"
)
@ProtoDoc("@Indexed")
public class HotRodUserSessionEntity extends AbstractHotRodEntity {

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1, required = true)
    public int entityVersion = 1;

    @ProtoField(number = 2, required = true)
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
    public Long started;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 12)
    public Long lastSessionRefresh;

    @ProtoField(number = 13)
    public Long expiration;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 14)
    public Set<HotRodStringPair> notes;

    @ProtoField(number = 15)
    public HotRodSessionState state;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 16)
    public Set<HotRodStringPair> authenticatedClientSessions;

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
