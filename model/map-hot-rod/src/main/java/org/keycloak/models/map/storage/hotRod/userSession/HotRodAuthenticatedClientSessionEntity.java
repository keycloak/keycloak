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
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity"
)
@ProtoDoc("@Indexed")
public class HotRodAuthenticatedClientSessionEntity extends AbstractHotRodEntity {

    @ProtoField(number = 1)
    public String id;

    @ProtoField(number = 2)
    public String realmId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String clientId;

    @ProtoField(number = 4)
    public String authMethod;

    @ProtoField(number = 5)
    public String redirectUri;

    @ProtoField(number = 6)
    public Long timestamp;

    @ProtoField(number = 7)
    public Long expiration;

    @ProtoField(number = 8)
    public String action;

    @ProtoField(number = 9)
    public Set<HotRodPair<String, String>> notes;

    @ProtoField(number = 10)
    public String currentRefreshToken;

    @ProtoField(number = 11)
    public Integer currentRefreshTokenUseCount;

    @ProtoField(number = 12)
    public Boolean offline;

    @Override
    public boolean equals(Object o) {
        return HotRodAuthenticatedClientSessionEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodAuthenticatedClientSessionEntityDelegate.entityHashCode(this);
    }
}
