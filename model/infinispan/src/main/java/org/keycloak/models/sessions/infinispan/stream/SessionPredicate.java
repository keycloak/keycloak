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

package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.function.Predicate;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@SerializeWith(SessionPredicate.ExternalizerImpl.class)
public class SessionPredicate<S extends SessionEntity> implements Predicate<Map.Entry<String, SessionEntityWrapper<S>>> {

    private final String realm;

    private SessionPredicate(String realm) {
        this.realm = realm;
    }

    public static SessionPredicate create(String realm) {
        return new SessionPredicate(realm);
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntityWrapper<S>> entry) {
        return realm.equals(entry.getValue().getEntity().getRealmId());
    }

    public static class ExternalizerImpl implements Externalizer<SessionPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, SessionPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realm, output);

        }

        @Override
        public SessionPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public SessionPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            SessionPredicate res = new SessionPredicate(MarshallUtil.unmarshallString(input));
            return res;
        }
    }
}
