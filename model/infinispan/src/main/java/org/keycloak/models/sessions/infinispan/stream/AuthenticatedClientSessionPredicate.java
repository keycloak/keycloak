/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(AuthenticatedClientSessionPredicate.ExternalizerImpl.class)
public class AuthenticatedClientSessionPredicate implements Predicate<Map.Entry<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>>> {

    private final String realm;

    private Integer expired;

    private AuthenticatedClientSessionPredicate(String realm) {
        this.realm = realm;
    }

    /**
     * Creates a client session predicate.
     * @param realm
     * @return
     */
    public static AuthenticatedClientSessionPredicate create(String realm) {
        return new AuthenticatedClientSessionPredicate(realm);
    }


    public AuthenticatedClientSessionPredicate expired(Integer expired) {
        this.expired = expired;
        return this;
    }


    @Override
    public boolean test(Map.Entry<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> entry) {
        AuthenticatedClientSessionEntity entity = entry.getValue().getEntity();

        if (!realm.equals(entity.getRealmId())) {
            return false;
        }

        if (expired != null && entity.getTimestamp() > expired) {
            return false;
        }

        return true;
    }


    public static class ExternalizerImpl implements Externalizer<AuthenticatedClientSessionPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, AuthenticatedClientSessionPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realm, output);
            KeycloakMarshallUtil.marshall(obj.expired, output);
        }

        @Override
        public AuthenticatedClientSessionPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public AuthenticatedClientSessionPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            AuthenticatedClientSessionPredicate res = new AuthenticatedClientSessionPredicate(MarshallUtil.unmarshallString(input));
            res.expired(KeycloakMarshallUtil.unmarshallInteger(input));
            return res;
        }
    }
}
