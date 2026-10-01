/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Set;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(Marshalling.USER_ISSUED_VERIFIABLE_CREDENTIALS_UPDATED_EVENT)
public class UserIssuedVerifiableCredentialsUpdatedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private static final String ALL_USERS = "all-users";

    private final boolean allUsers;

    private UserIssuedVerifiableCredentialsUpdatedEvent(String id, boolean allUsers) {
        super(id);
        this.allUsers = allUsers;
    }

    @ProtoFactory
    static UserIssuedVerifiableCredentialsUpdatedEvent create(String id, boolean allUsers) {
        return new UserIssuedVerifiableCredentialsUpdatedEvent(id, allUsers);
    }

    public static UserIssuedVerifiableCredentialsUpdatedEvent create(String userId) {
        return create(userId, false);
    }

    public static UserIssuedVerifiableCredentialsUpdatedEvent createAll() {
        return create(ALL_USERS, true);
    }

    @ProtoField(2)
    boolean isAllUsers() {
        return allUsers;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Boolean.hashCode(allUsers);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj)
                && allUsers == ((UserIssuedVerifiableCredentialsUpdatedEvent) obj).allUsers;
    }

    @Override
    public String toString() {
        return String.format("UserIssuedVerifiableCredentialsUpdatedEvent [ userId=%s, allUsers=%s ]", getId(), allUsers);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        if (allUsers) {
            userCache.allIssuedVerifiableCredentialsInvalidation(invalidations);
        } else {
            userCache.issuedVerifiableCredentialsInvalidation(getId(), invalidations);
        }
    }
}
