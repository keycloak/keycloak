/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage;

import java.security.SecureRandom;
import java.util.UUID;

/**
 *  Converts given storage key from and to {@code String} representation.
 *
 *  @author hmlnarik
 */
public interface StringKeyConvertor<K> {

    /**
     * Returns String representation of the key from native representation
     * @param key 
     * @throws IllegalArgumentException if the string format is not recognized
     * @throws NullPointerException if the parameter is {@code null}
     * @return See above
     */
    default String keyToString(K key)   { return key == null ? null : key.toString(); }

    /**
     * Returns a new unique primary key for the storage that
     * this {@code StringKeyConvertor} belongs to. The uniqueness
     * needs to be guaranteed by e.g. using database sequences or
     * using a random value that is proved sufficiently improbable
     * to be repeated.
     * 
     * @return
     */
    K yieldNewUniqueKey();

    /**
     * Returns native representation of the key from String representation
     * @param key
     * @throws IllegalArgumentException if the string format is not recognized
     * @throws NullPointerException if the parameter is {@code null}
     * @return See above
     */
    K fromString(String key);

    /**
     * Exception-free variant of {@link #fromString} method.
     * Returns native representation of the key from String representation,
     * or {@code null} if the {@code key} is either {@code null} or invalid.
     * @param key
     * @return See above
     */
    default K fromStringSafe(String key) {
        try {
            return fromString(key);
        } catch (Exception ex) {
            return null;
        }
    }

    public static class UUIDKey implements StringKeyConvertor<UUID> {

        public static final UUIDKey INSTANCE = new UUIDKey();

        @Override
        public UUID yieldNewUniqueKey() {
            return UUID.randomUUID();
        }

        @Override
        public UUID fromString(String key) {
            return UUID.fromString(key);
        }
    }

    public static class StringKey implements StringKeyConvertor<String> {

        public static final StringKey INSTANCE = new StringKey();

        @Override
        public String fromString(String key) {
            return key;
        }

        @Override
        public String yieldNewUniqueKey() {
            return fromString(UUID.randomUUID().toString());
        }
    }

    public static class ULongKey implements StringKeyConvertor<Long> {

        public static final ULongKey INSTANCE = new ULongKey();

        /*
         * The random number generator used by this class to create random
         * based UUIDs. In a holder class to defer initialization until needed.
         */
        private static class Holder {
            static final SecureRandom numberGenerator = new SecureRandom();
        }

        @Override
        public Long fromString(String key) {
            return key == null ? null : Long.parseUnsignedLong(key);
        }

        @Override
        public Long yieldNewUniqueKey() {
            return Holder.numberGenerator.nextLong();
        }
    }

}
