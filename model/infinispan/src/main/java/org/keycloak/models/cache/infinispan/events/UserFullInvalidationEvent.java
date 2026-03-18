/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Used when user added/removed
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.USER_FULL_INVALIDATION_EVENT)
public class UserFullInvalidationEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    @ProtoField(2)
    final String username;
    @ProtoField(3)
    final String email;
    @ProtoField(4)
    final String realmId;
    @ProtoField(5)
    final boolean identityFederationEnabled;
    @ProtoField(value = 6, mapImplementation = HashMap.class)
    final Map<String, String> federatedIdentities;

    private UserFullInvalidationEvent(String id, String username, String email, String realmId, boolean identityFederationEnabled, Map<String, String> federatedIdentities) {
        super(id);
        this.username = Objects.requireNonNull(username);
        this.email = email;
        this.realmId = Objects.requireNonNull(realmId);
        this.federatedIdentities = federatedIdentities;
        this.identityFederationEnabled = identityFederationEnabled;
    }

    public static UserFullInvalidationEvent create(String userId, String username, String email, String realmId, boolean identityFederationEnabled, Stream<FederatedIdentityModel> federatedIdentities) {
        Map<String, String> federatedIdentitiesMap = null;
        if (identityFederationEnabled) {
            federatedIdentitiesMap = federatedIdentities.collect(Collectors.toMap(FederatedIdentityModel::getIdentityProvider,
                    FederatedIdentityModel::getUserId));
        }
        return new UserFullInvalidationEvent(userId, username, email, realmId, identityFederationEnabled, federatedIdentitiesMap);
    }

    @ProtoFactory
    static UserFullInvalidationEvent protoFactory(String id, String username, String email, String realmId, boolean identityFederationEnabled, Map<String, String> federatedIdentities) {
        return new UserFullInvalidationEvent(id, username, email, realmId, identityFederationEnabled, federatedIdentities);
    }

    public Map<String, String> getFederatedIdentities() {
        return federatedIdentities;
    }

    @Override
    public String toString() {
        return String.format("UserFullInvalidationEvent [ userId=%s, username=%s, email=%s ]", getId(), username, email);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.fullUserInvalidation(getId(), username, email, realmId, identityFederationEnabled, federatedIdentities, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserFullInvalidationEvent that = (UserFullInvalidationEvent) o;
        return identityFederationEnabled == that.identityFederationEnabled && Objects.equals(username, that.username) && Objects.equals(email, that.email) && Objects.equals(realmId, that.realmId) && Objects.equals(federatedIdentities, that.federatedIdentities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), username, email, realmId, identityFederationEnabled, federatedIdentities);
    }

}
