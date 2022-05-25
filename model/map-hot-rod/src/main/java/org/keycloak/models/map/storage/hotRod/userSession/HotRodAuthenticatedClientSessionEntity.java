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
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.userSession.HotRodAuthenticatedClientSessionEntity.AbstractHotRodAuthenticatedClientSessionEntityDelegate"
)
@ProtoDoc("@Indexed")
public class HotRodAuthenticatedClientSessionEntity extends AbstractHotRodEntity {

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = 1;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 2)
    public String id;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String userSessionId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 4)
    public String realmId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 5)
    public String clientId;

    @ProtoField(number = 6)
    public String authMethod;

    @ProtoField(number = 7)
    public String redirectUri;

    @ProtoField(number = 8)
    public Long timestamp;

    @ProtoField(number = 9)
    public Long expiration;

    @ProtoField(number = 10)
    public String action;

    @ProtoField(number = 11)
    public Set<HotRodPair<String, String>> notes;

    @ProtoField(number = 12)
    public String currentRefreshToken;

    @ProtoField(number = 13)
    public Integer currentRefreshTokenUseCount;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 14)
    public Boolean offline;

    public static abstract class AbstractHotRodAuthenticatedClientSessionEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodAuthenticatedClientSessionEntity> implements MapAuthenticatedClientSessionEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodAuthenticatedClientSessionEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodAuthenticatedClientSessionEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodAuthenticatedClientSessionEntityDelegate.entityHashCode(this);
    }
}
