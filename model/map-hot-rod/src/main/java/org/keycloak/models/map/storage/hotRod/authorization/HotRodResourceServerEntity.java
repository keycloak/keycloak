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

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;


@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.authorization.entity.MapResourceServerEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceServerEntity.AbstractHotRodResourceServerEntity"
)
@ProtoDoc("@Indexed")
public class HotRodResourceServerEntity extends AbstractHotRodEntity {

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1, required = true)
    public int entityVersion = 1;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 2)
    public String id;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String realmId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 4)
    public String clientId;

    @ProtoField(number = 5)
    public Boolean allowRemoteResourceManagement;

    @ProtoField(number = 6)
    public HotRodPolicyEnforcementMode policyEnforcementMode;

    @ProtoField(number = 7)
    public HotRodDecisionStrategy decisionStrategy;

    public static abstract class AbstractHotRodResourceServerEntity extends UpdatableHotRodEntityDelegateImpl<HotRodResourceServerEntity> implements MapResourceServerEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodResourceServerEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodResourceServerEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodResourceServerEntityDelegate.entityHashCode(this);
    }
}
