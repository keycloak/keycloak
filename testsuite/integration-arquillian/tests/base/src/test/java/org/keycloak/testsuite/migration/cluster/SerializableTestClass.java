/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.migration.cluster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

@SerializeWith(SerializableTestClass.ExternalizerImpl.class)
public class SerializableTestClass implements Serializable {
    public static class ExternalizerImpl implements Externalizer<SerializableTestClass> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput oo, SerializableTestClass t) throws IOException {
            oo.writeByte(VERSION_1);
        }

        @Override
        public SerializableTestClass readObject(ObjectInput oi) throws IOException, ClassNotFoundException {
            switch (oi.readByte()) {
                case VERSION_1:
                    return new SerializableTestClass();
                default:
                    throw new IOException("Unknown version");
            }
        }
    }
}
