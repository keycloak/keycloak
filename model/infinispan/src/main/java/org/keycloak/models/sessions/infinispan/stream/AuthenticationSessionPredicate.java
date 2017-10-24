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

import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
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
@SerializeWith(AuthenticationSessionPredicate.ExternalizerImpl.class)
public class AuthenticationSessionPredicate implements Predicate<Map.Entry<String, AuthenticationSessionEntity>> {

    private final String realm;

    private String client;

    private String user;

    private Integer expired;

    //private String brokerSessionId;
    //private String brokerUserId;

    private AuthenticationSessionPredicate(String realm) {
        this.realm = realm;
    }

    public static AuthenticationSessionPredicate create(String realm) {
        return new AuthenticationSessionPredicate(realm);
    }

    public AuthenticationSessionPredicate user(String user) {
        this.user = user;
        return this;
    }

    public AuthenticationSessionPredicate client(String client) {
        this.client = client;
        return this;
    }

    public AuthenticationSessionPredicate expired(Integer expired) {
        this.expired = expired;
        return this;
    }

//    public UserSessionPredicate brokerSessionId(String id) {
//        this.brokerSessionId = id;
//        return this;
//    }

//    public UserSessionPredicate brokerUserId(String id) {
//        this.brokerUserId = id;
//        return this;
//    }

    @Override
    public boolean test(Map.Entry<String, AuthenticationSessionEntity> entry) {
        AuthenticationSessionEntity entity = entry.getValue();

        if (!realm.equals(entity.getRealmId())) {
            return false;
        }

        if (user != null && !entity.getAuthUserId().equals(user)) {
            return false;
        }

        if (client != null && !entity.getClientUuid().equals(client)) {
            return false;
        }

//        if (brokerSessionId != null && !brokerSessionId.equals(entity.getBrokerSessionId())) {
//            return false;
//        }
//
//        if (brokerUserId != null && !brokerUserId.equals(entity.getBrokerUserId())) {
//            return false;
//        }

        if (expired != null && entity.getTimestamp() > expired) {
            return false;
        }

        return true;
    }

    public static class ExternalizerImpl implements Externalizer<AuthenticationSessionPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, AuthenticationSessionPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realm, output);
            MarshallUtil.marshallString(obj.user, output);
            MarshallUtil.marshallString(obj.client, output);
            KeycloakMarshallUtil.marshall(obj.expired, output);

        }

        @Override
        public AuthenticationSessionPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public AuthenticationSessionPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            AuthenticationSessionPredicate res = new AuthenticationSessionPredicate(MarshallUtil.unmarshallString(input));
            res.user(MarshallUtil.unmarshallString(input));
            res.client(MarshallUtil.unmarshallString(input));
            res.expired(KeycloakMarshallUtil.unmarshallInteger(input));
            return res;
        }
    }
}
