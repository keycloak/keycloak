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

import java.util.Objects;
import java.util.Set;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.USER_UPDATED_EVENT)
public class UserUpdatedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    @ProtoField(2)
    final String username;
    @ProtoField(3)
    final String email;
    @ProtoField(4)
    final String realmId;

    private UserUpdatedEvent(String id, String username, String email, String realmId) {
        super(id);
        this.username = Objects.requireNonNull(username);
        this.email = email;
        this.realmId = Objects.requireNonNull(realmId);
    }

    public static UserUpdatedEvent create(String id, String username, String email, String realmId) {
        return new UserUpdatedEvent(id, username, email, realmId);
    }

    @ProtoFactory
    static UserUpdatedEvent protoFactory(String id, String username, String email, String realmId) {
        return new UserUpdatedEvent(id, username, email, realmId);
    }

    @Override
    public String toString() {
        return String.format("UserUpdatedEvent [ userId=%s, username=%s, email=%s ]", getId(), username, email);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.userUpdatedInvalidations(getId(), username, email, realmId, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserUpdatedEvent that = (UserUpdatedEvent) o;
        return Objects.equals(username, that.username) && Objects.equals(email, that.email) && Objects.equals(realmId, that.realmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), username, email, realmId);
    }

}
