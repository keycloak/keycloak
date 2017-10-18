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

package org.keycloak.models.sessions.infinispan.entities;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@SerializeWith(LoginFailureKey.ExternalizerImpl.class)
public class LoginFailureKey {

    private final String realmId;
    private final String userId;

    public LoginFailureKey(String realmId, String userId) {
        this.realmId = realmId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoginFailureKey key = (LoginFailureKey) o;

        if (realmId != null ? !realmId.equals(key.realmId) : key.realmId != null) return false;
        if (userId != null ? !userId.equals(key.userId) : key.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = realmId != null ? realmId.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return String.format("LoginFailureKey [ realmId=%s. userId=%s ]", realmId, userId);
    }

    public static class ExternalizerImpl implements Externalizer<LoginFailureKey> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, LoginFailureKey value) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(value.realmId, output);
            MarshallUtil.marshallString(value.userId, output);
        }

        @Override
        public LoginFailureKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public LoginFailureKey readObjectVersion1(ObjectInput input) throws IOException {
            return new LoginFailureKey(MarshallUtil.unmarshallString(input), MarshallUtil.unmarshallString(input));
        }
    }
}
