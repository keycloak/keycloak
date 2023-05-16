package org.freedesktop.dbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DBusMap<K, V> implements Map<K, V> {
    // CHECKSTYLE:OFF
    Object[][] entries;
    // CHECKSTYLE:ON
    public DBusMap(Object[][] _entries) {
        this.entries = _entries;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object _key) {
        for (Object[] entry : entries) {
            if (Objects.equals(_key, entry[0])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object _value) {
        for (Object[] entry : entries) {
            if (Objects.equals(_value, entry[1])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> s = new LinkedHashSet<>();
        for (int i = 0; i < entries.length; i++) {
            s.add(new Entry(i));
        }
        return s;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object _key) {
        for (Object[] entry : entries) {
            if (_key == entry[0] || _key != null && _key.equals(entry[0])) {
                return (V) entry[1];
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return entries.length == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        Set<K> s = new LinkedHashSet<>();
        for (Object[] entry : entries) {
            s.add((K) entry[0]);
        }
        return s;
    }

    @Override
    public V put(K _key, V _value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> _t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object _key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return entries.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> values() {
        List<V> l = new ArrayList<>();
        for (Object[] entry : entries) {
            l.add((V) entry[1]);
        }
        return l;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(entries);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object _o) {
        if (null == _o) {
            return false;
        }
        if (!(_o instanceof Map)) {
            return false;
        }
        return ((Map<K, V>) _o).entrySet().equals(entrySet());
    }

    @Override
    public String toString() {
        String sb = "{"
                + Arrays.stream(entries).map(e -> e[0] + " => " + e[1])
                    .collect(Collectors.joining(","))
                + "}";

        return sb;
    }

    class Entry implements Map.Entry<K, V>, Comparable<Entry> {
        private final int entry;

        Entry(int _i) {
            this.entry = _i;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object _o) {
            if (null == _o) {
                return false;
            }
            if (!(_o instanceof DBusMap.Entry)) {
                return false;
            }
            return this.entry == ((Entry) _o).entry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) entries[entry][0];
        }

        @Override
        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) entries[entry][1];
        }

        @Override
        public int hashCode() {
            return entries[entry][0].hashCode();
        }

        @Override
        public V setValue(V _value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Entry _e) {
            return entry - _e.entry;
        }
    }
}
