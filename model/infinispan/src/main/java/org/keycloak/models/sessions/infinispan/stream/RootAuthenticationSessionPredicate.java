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

import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(RootAuthenticationSessionPredicate.ExternalizerImpl.class)
public class RootAuthenticationSessionPredicate implements Predicate<Map.Entry<String, RootAuthenticationSessionEntity>> {

    private final String realm;

    private Integer expired;

    private RootAuthenticationSessionPredicate(String realm) {
        this.realm = realm;
    }

    public static RootAuthenticationSessionPredicate create(String realm) {
        return new RootAuthenticationSessionPredicate(realm);
    }

    public RootAuthenticationSessionPredicate expired(Integer expired) {
        this.expired = expired;
        return this;
    }


    @Override
    public boolean test(Map.Entry<String, RootAuthenticationSessionEntity> entry) {
        RootAuthenticationSessionEntity entity = entry.getValue();

        if (!realm.equals(entity.getRealmId())) {
            return false;
        }

        if (expired != null && entity.getTimestamp() > expired) {
            return false;
        }

        return true;
    }

    public static class ExternalizerImpl implements Externalizer<RootAuthenticationSessionPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, RootAuthenticationSessionPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realm, output);
            KeycloakMarshallUtil.marshall(obj.expired, output);

        }

        @Override
        public RootAuthenticationSessionPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public RootAuthenticationSessionPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            RootAuthenticationSessionPredicate res = new RootAuthenticationSessionPredicate(MarshallUtil.unmarshallString(input));
            res.expired(KeycloakMarshallUtil.unmarshallInteger(input));
            return res;
        }
    }
}
