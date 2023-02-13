/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

/**
 * Provides a long =&gt; MethodCall map which doesn't allocate objects
 * on insertion/removal. Keys must be inserted in ascending order.
 */
class EfficientMap {
    private long[] kv;
    private MethodCall[] vv;
    private int start;
    private int end;
    private int init_size;

    public EfficientMap(int initial_size) {
        init_size = initial_size;
        shrink();
    }

    private void grow() {
        // create new vectors twice as long
        long[] oldkv = kv;
        kv = new long[oldkv.length * 2];
        MethodCall[] oldvv = vv;
        vv = new MethodCall[oldvv.length * 2];

        // copy start->length to the start of the new vector
        System.arraycopy(oldkv, start, kv, 0, oldkv.length - start);
        System.arraycopy(oldvv, start, vv, 0, oldvv.length - start);
        // copy 0->end to the next part of the new vector
        if (end != (oldkv.length - 1)) {
            System.arraycopy(oldkv, 0, kv, oldkv.length - start, end + 1);
            System.arraycopy(oldvv, 0, vv, oldvv.length - start, end + 1);
        }
        // reposition pointers
        start = 0;
        end = oldkv.length;
    }

    // create a new vector with just the valid keys in and return it
    public long[] getKeys() {
        int size;
        if (start < end) size = end - start;
        else size = kv.length - (start - end);
        long[] lv = new long[size];
        int copya;
        if (size > kv.length - start) copya = kv.length - start;
        else copya = size;
        System.arraycopy(kv, start, lv, 0, copya);
        if (copya < size) {
            System.arraycopy(kv, 0, lv, copya, size - copya);
        }
        return lv;
    }

    private void shrink() {
        if (null != kv && kv.length == init_size) return;
        // reset to original size
        kv = new long[init_size];
        vv = new MethodCall[init_size];
        start = 0;
        end = 0;
    }

    public void put(long l, MethodCall m) {
        // put this at the end
        kv[end] = l;
        vv[end] = m;
        // move the end
        if (end == (kv.length - 1)) end = 0;
        else end++;
        // if we are out of space, grow.
        if (end == start) grow();
    }

    public MethodCall remove(long l) {
        // find the item
        int pos = find(l);
        // if we don't have it return null
        if (-1 == pos) return null;
        // get the value
        MethodCall m = vv[pos];
        // set it as unused
        vv[pos] = null;
        kv[pos] = -1;
        // move the pointer to the first full element
        while (-1 == kv[start]) {
            if (start == (kv.length - 1)) start = 0;
            else start++;
            // if we have emptied the list, shrink it
            if (start == end) {
                shrink();
                break;
            }
        }
        return m;
    }

    public boolean contains(long l) {
        // check if find succeeds
        return -1 != find(l);
    }

    /* could binary search, but it's probably the first one */
    private int find(long l) {
        int i = start;
        while (i != end && kv[i] != l)
            if (i == (kv.length - 1)) i = 0;
            else i++;
        if (i == end) return -1;
        return i;
    }
}
