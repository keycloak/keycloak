/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import cx.ath.matthew.debug.Debug;

/**
 * Provides a Message queue which doesn't allocate objects
 * on insertion/removal.
 */
class EfficientQueue {
    private Message[] mv;
    private int start;
    private int end;
    private int init_size;

    public EfficientQueue(int initial_size) {
        init_size = initial_size;
        shrink();
    }

    private void grow() {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Growing");
        // create new vectors twice as long
        Message[] oldmv = mv;
        mv = new Message[oldmv.length * 2];

        // copy start->length to the start of the new vector
        System.arraycopy(oldmv, start, mv, 0, oldmv.length - start);
        // copy 0->end to the next part of the new vector
        if (end != (oldmv.length - 1)) {
            System.arraycopy(oldmv, 0, mv, oldmv.length - start, end + 1);
        }
        // reposition pointers
        start = 0;
        end = oldmv.length;
    }

    // create a new vector with just the valid keys in and return it
    public Message[] getKeys() {
        if (start == end) return new Message[0];
        Message[] lv;
        if (start < end) {
            int size = end - start;
            lv = new Message[size];
            System.arraycopy(mv, start, lv, 0, size);
        } else {
            int size = mv.length - start + end;
            lv = new Message[size];
            System.arraycopy(mv, start, lv, 0, mv.length - start);
            System.arraycopy(mv, 0, lv, mv.length - start, end);
        }
        return lv;
    }

    private void shrink() {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Shrinking");
        if (null != mv && mv.length == init_size) return;
        // reset to original size
        mv = new Message[init_size];
        start = 0;
        end = 0;
    }

    public void add(Message m) {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Enqueueing Message " + m);
        // put this at the end
        mv[end] = m;
        // move the end
        if (end == (mv.length - 1)) end = 0;
        else end++;
        // if we are out of space, grow.
        if (end == start) grow();
    }

    public Message remove() {
        if (start == end) return null;
        // find the item
        int pos = start;
        // get the value
        Message m = mv[pos];
        // set it as unused
        mv[pos] = null;
        if (start == (mv.length - 1)) start = 0;
        else start++;
        if (Debug.debug) Debug.print(Debug.DEBUG, "Dequeueing " + m);
        return m;
    }

    public boolean isEmpty() {
        // check if find succeeds
        return start == end;
    }

    public int size() {
        if (end >= start)
            return end - start;
        else
            return mv.length - start + end;
    }
}
