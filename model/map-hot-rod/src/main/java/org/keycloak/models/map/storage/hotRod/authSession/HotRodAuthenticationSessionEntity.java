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

package org.keycloak.models.map.storage.hotRod.authSession;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.authSession.MapAuthenticationSessionEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.authSession.MapAuthenticationSessionEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.authSession.HotRodAuthenticationSessionEntity.AbstractHotRodAuthenticationSessionEntityDelegate"
)
public class HotRodAuthenticationSessionEntity extends AbstractHotRodEntity {

    @ProtoField(number = 1)
    public String tabId;

    @ProtoField(number = 2)
    public String clientUUID;

    @ProtoField(number = 3)
    public String authUserId;

    @ProtoField(number = 4)
    public Long timestamp;

    @ProtoField(number = 5)
    public String redirectUri;

    @ProtoField(number = 6)
    public String action;

    @ProtoField(number = 7)
    public Set<String> clientScopes;

    @ProtoField(number = 8)
    public Set<HotRodPair<String, Integer>> executionStatuses;

    @ProtoField(number = 9)
    public String protocol;

    @ProtoField(number = 10)
    public Set<HotRodPair<String, String>> clientNotes;

    @ProtoField(number = 11)
    public Set<HotRodPair<String, String>> authNotes;

    @ProtoField(number = 12)
    public Set<String> requiredActions;

    @ProtoField(number = 13)
    public Set<HotRodPair<String, String>> userSessionNotes;

    public static abstract class AbstractHotRodAuthenticationSessionEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodAuthenticationSessionEntity> implements MapAuthenticationSessionEntity {

        @Override
        public Map<String, AuthenticationSessionModel.ExecutionStatus> getExecutionStatuses() {
            Set<HotRodPair<String, Integer>> executionStatuses = getHotRodEntity().executionStatuses;
            if (executionStatuses == null) {
                return Collections.emptyMap();
            }
            return executionStatuses.stream().collect(Collectors.toMap(HotRodPair::getKey,
                    v -> AuthenticationSessionModel.ExecutionStatus.valueOfInteger(v.getValue())));
        }

        @Override
        public void setExecutionStatuses(Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus) {
            HotRodAuthenticationSessionEntity hotRodEntity = getHotRodEntity();
            Set<HotRodPair<String, Integer>> executionStatusSet = executionStatus == null ? null :
                    executionStatus.entrySet().stream()
                            .map(e -> new HotRodPair<>(e.getKey(), e.getValue().getStableIndex()))
                            .collect(Collectors.toSet());
            hotRodEntity.updated |= ! Objects.equals(hotRodEntity.executionStatuses, executionStatusSet);
            hotRodEntity.executionStatuses = executionStatusSet;
        }

        @Override
        public void setExecutionStatus(String authenticator, AuthenticationSessionModel.ExecutionStatus status) {
            HotRodAuthenticationSessionEntity hotRodEntity = getHotRodEntity();
            if (hotRodEntity.executionStatuses == null) {
                hotRodEntity.executionStatuses = new HashSet<>();
            }
            boolean valueUndefined = status == null;
            hotRodEntity.updated |= HotRodTypesUtils.removeFromSetByMapKey(hotRodEntity.executionStatuses, authenticator, HotRodTypesUtils::getKey);
            hotRodEntity.updated |= !valueUndefined && hotRodEntity.executionStatuses.add(new HotRodPair<>(authenticator, status.getStableIndex()));
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodAuthenticationSessionEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodAuthenticationSessionEntityDelegate.entityHashCode(this);
    }
}
