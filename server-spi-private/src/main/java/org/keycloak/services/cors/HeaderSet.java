/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.cors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.common.util.CollectionUtil;

/**
 * A {@link Set} of {@link String} elements backed by a {@link TreeSet}, which provides sorting, deduplication
 * and caching of the string representation.
 *
 * The string representation, returned by {@link #toHeaderString}, is a concatenation of the elements with ", " as a
 * separator. It will be recomputed (and cached) every time the HeaderSet is modified.
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class HeaderSet implements Set<String> {

    private final TreeSet<String> set;
    private String string;

    public HeaderSet(Collection<String> c) {
        this.set = new TreeSet<>(c);
        update();
    }

    private HeaderSet(TreeSet<String> set, String string) {
        this.set = set;
        this.string = string;
    }

    /**
     * Returns a mutable copy of the given {@link HeaderSet}.
     *
     * @param hs source {@link HeaderSet}
     * @return a mutable copy of hs
     */
    @SuppressWarnings("unchecked")
    public static HeaderSet copyOf(HeaderSet hs) {
        return new HeaderSet((TreeSet<String>) hs.set.clone(), hs.string);
    }

    /**
     * Parses a comma-separated HTTP header list into a {@link HeaderSet}.
     *
     * @param s a comma-separated list of headers
     * @return a {@link HeaderSet}
     */
    public static HeaderSet parse(String s) {
        String[] split = s.split("[,\\s]+");
        return new HeaderSet(Arrays.asList(split));
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            private final Iterator<String> iterator = set.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
                update();
            }

        };
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean add(String s) {
        boolean added = set.add(s);
        if (added) update();
        return added;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = set.remove(o);
        if (removed) update();
        return removed;
    }

    @Override
    public boolean containsAll(Collection c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        boolean changed = set.addAll(c);
        if (changed) update();
        return changed;
    }

    @Override
    public boolean retainAll(Collection c) {
        boolean changed = set.retainAll(c);
        if (changed) update();
        return changed;
    }

    @Override
    public boolean removeAll(Collection c) {
        boolean changed = set.removeAll(c);
        if (changed) update();
        return changed;
    }

    @Override
    public void clear() {
        set.clear();
        update();
    }

    @Override
    public String toString() {
        return set.toString();
    }

    /**
     * Returns a concatenation of the elements with ", " as a separator. The value will be recomputed (and cached)
     * every time the @{link HeaderSet} is modified.
     *
     * @return string representation of the {@link HeaderSet}
     */
    public String toHeaderString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && o instanceof HeaderSet hs) {
            return set.equals(hs.set);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    private void update() {
        string = CollectionUtil.join(set);
    }

}
