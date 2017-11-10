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
@SerializeWith(UserConsentsUpdatedEvent.ExternalizerImpl.class)
public class UserConsentsUpdatedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private String userId;

    public static UserConsentsUpdatedEvent create(String userId) {
        UserConsentsUpdatedEvent event = new UserConsentsUpdatedEvent();
        event.userId = userId;
        return event;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String toString() {
        return String.format("UserConsentsUpdatedEvent [ userId=%s ]", userId);
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.consentInvalidation(userId, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<UserConsentsUpdatedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, UserConsentsUpdatedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.userId, output);
        }

        @Override
        public UserConsentsUpdatedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public UserConsentsUpdatedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            UserConsentsUpdatedEvent res = new UserConsentsUpdatedEvent();
            res.userId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
