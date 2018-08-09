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

package org.keycloak.models.sessions.infinispan.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.jboss.logging.Logger;

/**
 *
 * Helper to optimize marshalling/unmarhsalling of some types
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakMarshallUtil {

    private static final Logger log = Logger.getLogger(KeycloakMarshallUtil.class);

    public static final Externalizer<String> STRING_EXT = new StringExternalizer();

    public static final Externalizer<UUID> UUID_EXT = new Externalizer<UUID>() {
        @Override
        public void writeObject(ObjectOutput output, UUID uuid) throws IOException {
            MarshallUtil.marshallUUID(uuid, output, true);
        }

        @Override
        public UUID readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return MarshallUtil.unmarshallUUID(input, true);
        }
    };

    // MAP

    public static <K, V> void writeMap(Map<K, V> map, Externalizer<K> keyExternalizer, Externalizer<V> valueExternalizer, ObjectOutput output) throws IOException {
        if (map == null) {
            output.writeByte(0);
        } else {
            output.writeByte(1);

            // Copy the map as it can be updated concurrently
            Map<K, V> copy = new HashMap<>(map);
            //Map<K, V> copy = map;

            output.writeInt(copy.size());

            for (Map.Entry<K, V> entry : copy.entrySet()) {
                keyExternalizer.writeObject(output, entry.getKey());
                valueExternalizer.writeObject(output, entry.getValue());
            }
        }
    }

    public static <K, V, TYPED_MAP extends Map<K, V>> TYPED_MAP readMap(ObjectInput input,
                                                                        Externalizer<K> keyExternalizer, Externalizer<V> valueExternalizer,
                                                                        MarshallUtil.MapBuilder<K, V, TYPED_MAP> mapBuilder) throws IOException, ClassNotFoundException {
        byte b = input.readByte();
        if (b == 0) {
            return null;
        } else {

            int size = input.readInt();

            TYPED_MAP map = mapBuilder.build(size);

            for (int i=0 ; i<size ; i++) {
                K key = keyExternalizer.readObject(input);
                V value = valueExternalizer.readObject(input);

                map.put(key, value);
            }

            return map;
        }
    }

    // COLLECTION

    public static <E> void writeCollection(Collection<E> col, Externalizer<E> valueExternalizer, ObjectOutput output) throws IOException {
        if (col == null) {
            output.writeByte(0);
        } else {
            output.writeByte(1);

            // Copy the collection as it can be updated concurrently
            Collection<E> copy = new LinkedList<>(col);

            output.writeInt(copy.size());

            for (E entry : copy) {
                valueExternalizer.writeObject(output, entry);
            }
        }
    }

    public static <E, T extends Collection<E>> T readCollection(ObjectInput input, Externalizer<E> valueExternalizer,
                                          MarshallUtil.CollectionBuilder<E, T> colBuilder) throws ClassNotFoundException, IOException {
        byte b = input.readByte();
        if (b == 0) {
            return null;
        } else {

            int size = input.readInt();

            T col = colBuilder.build(size);

            for (int i=0 ; i<size ; i++) {
                E value = valueExternalizer.readObject(input);
                col.add(value);
            }

            return col;
        }
    }

    /**
     * Marshalls the given object with support of {@code null} values.
     * @param obj Object to marshall (can be {@code null})
     * @param output Output stream
     * @throws IOException
     */
    public static void marshall(Integer obj, ObjectOutput output) throws IOException {
        if (obj == null) {
            output.writeBoolean(false);
        } else {
            output.writeBoolean(true);
            output.writeInt(obj);
        }
    }

    /**
     * Unmarshals the given object into {@code Integer} instance.
     * @param input Input stream
     * @return Unmarshalled value (can be {@code null})
     * @throws IOException
     */
    public static Integer unmarshallInteger(ObjectInput input) throws IOException {
        boolean isSet = input.readBoolean();
        return isSet ? input.readInt() : null;
    }

    public static class ConcurrentHashMapBuilder<K, V> implements MarshallUtil.MapBuilder<K, V, ConcurrentHashMap<K, V>> {

        @Override
        public ConcurrentHashMap<K, V> build(int size) {
            return new ConcurrentHashMap<>(size);
        }

    }

    public static class HashSetBuilder<E> implements MarshallUtil.CollectionBuilder<E, HashSet<E>> {

        @Override
        public HashSet<E> build(int size) {
            return new HashSet<>(size);
        }
    }


    private static class StringExternalizer implements Externalizer<String> {

        @Override
        public void writeObject(ObjectOutput output, String str) throws IOException {
            MarshallUtil.marshallString(str, output);
        }

        @Override
        public String readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return MarshallUtil.unmarshallString(input);
        }

    }

}
