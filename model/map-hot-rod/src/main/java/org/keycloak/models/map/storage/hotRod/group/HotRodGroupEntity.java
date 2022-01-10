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

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodAttributeEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.group.MapGroupEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.group.HotRodGroupEntity.AbstractHotRodGroupEntityDelegate"
)
public class HotRodGroupEntity implements AbstractHotRodEntity {

    public static abstract class AbstractHotRodGroupEntityDelegate extends UpdatableEntity.Impl implements HotRodEntityDelegate<HotRodGroupEntity>, MapGroupEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodGroupEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            this.updated |= id != null;
        }
    }

    @ProtoField(number = 1, required = true)
    public int entityVersion = 1;

    @ProtoField(number = 2, required = true)
    public String id;

    @ProtoField(number = 3)
    public String realmId;

    @ProtoField(number = 4)
    public String name;

    @ProtoField(number = 5)
    public String parentId;

    @ProtoField(number = 6)
    public Set<HotRodAttributeEntity> attributes;

    @ProtoField(number = 7)
    public Set<String> grantedRoles;
}
