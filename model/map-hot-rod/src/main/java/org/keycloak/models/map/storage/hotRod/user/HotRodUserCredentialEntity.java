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

package org.keycloak.models.map.storage.hotRod.user;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.client.HotRodProtocolMapperEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

@GenerateHotRodEntityImplementation(implementInterface = "org.keycloak.models.map.user.MapUserCredentialEntity")
public class HotRodUserCredentialEntity extends AbstractHotRodEntity {

    @ProtoField(number = 1)
    public String id;

    @ProtoField(number = 2)
    public String type;

    @ProtoField(number = 3)
    public String userLabel;

    @ProtoField(number = 4)
    public Long createdDate;

    @ProtoField(number = 5)
    public String secretData;

    @ProtoField(number = 6)
    public String credentialData;

    @ProtoField(number = 7)
    public Integer priority;

    @Override
    public boolean equals(Object o) {
        return HotRodUserCredentialEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodUserCredentialEntityDelegate.entityHashCode(this);
    }
}
