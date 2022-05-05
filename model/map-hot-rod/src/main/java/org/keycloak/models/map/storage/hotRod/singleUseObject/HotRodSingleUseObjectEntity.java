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

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.singleUseObject.HotRodSingleUseObjectEntity.AbstractHotRodSingleUseObjectEntityDelegate"
)
@ProtoDoc("@Indexed")
public class HotRodSingleUseObjectEntity extends AbstractHotRodEntity {

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = 1;

    @ProtoField(number = 2)
    public String id;

    @ProtoField(number = 3)
    public String userId;

    @ProtoField(number = 4)
    public String actionId;

    @ProtoField(number = 5)
    public String actionVerificationNonce;

    @ProtoField(number = 6)
    public Long expiration;

    @ProtoField(number = 7)
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
