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
import cx.ath.matthew.unix.USOutputStream;
import cx.ath.matthew.utils.Hexdump;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MessageWriter {
    private OutputStream out;
    private boolean isunix;

    public MessageWriter(OutputStream out) {
        this.out = out;
        this.isunix = false;
        try {
            if (out instanceof USOutputStream)
                this.isunix = true;
        } catch (Throwable t) {
        }
        if (!this.isunix)
            this.out = new BufferedOutputStream(this.out);
    }

    public void writeMessage(Message m) throws IOException {
        if (Debug.debug) {
            Debug.print(Debug.INFO, "<= " + m);
        }
        if (null == m) return;
        if (null == m.getWireData()) {
            if (Debug.debug) Debug.print(Debug.WARN, "Message " + m + " wire-data was null!");
            return;
        }
        if (isunix) {
            if (Debug.debug) {
                Debug.print(Debug.DEBUG, "Writing all " + m.getWireData().length + " buffers simultaneously to Unix Socket");
                for (byte[] buf : m.getWireData())
                    Debug.print(Debug.VERBOSE, "(" + buf + "):" + (null == buf ? "" : Hexdump.format(buf)));
            }
            ((USOutputStream) out).write(m.getWireData());
        } else
            for (byte[] buf : m.getWireData()) {
                if (Debug.debug)
                    Debug.print(Debug.VERBOSE, "(" + buf + "):" + (null == buf ? "" : Hexdump.format(buf)));
                if (null == buf) break;
                out.write(buf);
            }
        out.flush();
    }

    public void close() throws IOException {
        if (Debug.debug) Debug.print(Debug.INFO, "Closing Message Writer");
        out.close();
    }
}
