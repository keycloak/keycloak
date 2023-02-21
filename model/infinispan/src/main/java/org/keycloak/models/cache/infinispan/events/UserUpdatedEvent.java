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

import org.keycloak.models.cache.infinispan.UserCacheManager;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(UserUpdatedEvent.ExternalizerImpl.class)
public class UserUpdatedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private String userId;
    private String username;
    private String email;
    private String realmId;

    public static UserUpdatedEvent create(String userId, String username, String email, String realmId) {
        UserUpdatedEvent event = new UserUpdatedEvent();
        event.userId = userId;
        event.username = username;
        event.email = email;
        event.realmId = realmId;
        return event;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String toString() {
        return String.format("UserUpdatedEvent [ userId=%s, username=%s, email=%s ]", userId, username, email);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.userUpdatedInvalidations(userId, username, email, realmId, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserUpdatedEvent that = (UserUpdatedEvent) o;
        return Objects.equals(userId, that.userId) && Objects.equals(username, that.username) && Objects.equals(email, that.email) && Objects.equals(realmId, that.realmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, username, email, realmId);
    }

    public static class ExternalizerImpl implements Externalizer<UserUpdatedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, UserUpdatedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.userId, output);
            MarshallUtil.marshallString(obj.username, output);
            MarshallUtil.marshallString(obj.email, output);
            MarshallUtil.marshallString(obj.realmId, output);
        }

        @Override
        public UserUpdatedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public UserUpdatedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            UserUpdatedEvent res = new UserUpdatedEvent();
            res.userId = MarshallUtil.unmarshallString(input);
            res.username = MarshallUtil.unmarshallString(input);
            res.email = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
