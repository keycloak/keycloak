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

package org.keycloak.models.sessions.infinispan.changes.sessions;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(SessionData.ExternalizerImpl.class)
public class SessionData {

    private final String realmId;
    private final int lastSessionRefresh;

    public SessionData(String realmId, int lastSessionRefresh) {
        this.realmId = realmId;
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public String getRealmId() {
        return realmId;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    @Override
    public String toString() {
        return String.format("realmId: %s, lastSessionRefresh: %d", realmId, lastSessionRefresh);
    }

    public static class ExternalizerImpl implements Externalizer<SessionData> {


        @Override
        public void writeObject(ObjectOutput output, SessionData obj) throws IOException {
            MarshallUtil.marshallString(obj.realmId, output);
            KeycloakMarshallUtil.marshall(obj.lastSessionRefresh, output);
        }


        @Override
        public SessionData readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            int lastSessionRefresh = KeycloakMarshallUtil.unmarshallInteger(input);

            return new SessionData(realmId, lastSessionRefresh);
        }

    }
}
