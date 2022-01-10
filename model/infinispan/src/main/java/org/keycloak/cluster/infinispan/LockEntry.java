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

package org.keycloak.cluster.infinispan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(LockEntry.ExternalizerImpl.class)
public class LockEntry implements Serializable {

    private String node;
    private int timestamp;

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public static class ExternalizerImpl implements Externalizer<LockEntry> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, LockEntry obj) throws IOException {
            output.writeByte(VERSION_1);
            MarshallUtil.marshallString(obj.node, output);
            KeycloakMarshallUtil.marshall(obj.timestamp, output);
        }

        @Override
        public LockEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public LockEntry readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            LockEntry entry = new LockEntry();
            entry.setNode(MarshallUtil.unmarshallString(input));
            entry.setTimestamp(KeycloakMarshallUtil.unmarshallInteger(input));
            return entry;
        }
    }
}
