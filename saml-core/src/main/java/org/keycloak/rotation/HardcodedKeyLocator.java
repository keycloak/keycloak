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

package org.keycloak.rotation;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Key locator that always returns a specified key.
 *
 * @author <a href="mailto:hmlnarik@redhat.com">Hynek Mlnařík</a>
 */
public class HardcodedKeyLocator implements KeyLocator, Iterable<Key> {

    private final Collection<? extends Key> keys;

    public HardcodedKeyLocator(Key key) {
        this.keys = Collections.singleton(key);
    }

    public HardcodedKeyLocator(Collection<? extends Key> keys) {
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        this.keys = new LinkedList<>(keys);
    }

    @Override
    public Key getKey(String kid) {
        if (this.keys.size() == 1) {
            return this.keys.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public void refreshKeyCache() {
        // do nothing
    }

    @Override
    public String toString() {
        return "hardcoded keys, count: " + this.keys.size();
    }

    @Override
    public Iterator<Key> iterator() {
        return (Iterator<Key>) Collections.unmodifiableCollection(keys).iterator();
    }
}
