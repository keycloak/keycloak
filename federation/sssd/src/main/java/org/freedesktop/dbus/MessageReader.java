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
import cx.ath.matthew.utils.Hexdump;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageProtocolVersionException;
import org.freedesktop.dbus.exceptions.MessageTypeException;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;

import static org.freedesktop.dbus.Gettext.getString;

public class MessageReader {
    private InputStream in;
    private byte[] buf = null;
    private byte[] tbuf = null;
    private byte[] header = null;
    private byte[] body = null;
    private int[] len = new int[4];

    public MessageReader(InputStream in) {
        this.in = new BufferedInputStream(in);
    }

    public Message readMessage() throws IOException, DBusException {
        int rv;
      /* Read the 12 byte fixed header, retrying as neccessary */
        if (null == buf) {
            buf = new byte[12];
            len[0] = 0;
        }
        if (len[0] < 12) {
            try {
                rv = in.read(buf, len[0], 12 - len[0]);
            } catch (SocketTimeoutException STe) {
                return null;
            }
            if (-1 == rv) throw new EOFException(getString("transportReturnedEOF"));
            len[0] += rv;
        }
        if (len[0] == 0) return null;
        if (len[0] < 12) {
            if (Debug.debug) Debug.print(Debug.DEBUG, "Only got " + len[0] + " of 12 bytes of header");
            return null;
        }

      /* Parse the details from the header */
        byte endian = buf[0];
        byte type = buf[1];
        byte protover = buf[3];
        if (protover > Message.PROTOCOL) {
            buf = null;
            throw new MessageProtocolVersionException(MessageFormat.format(getString("protocolVersionUnsupported"), new Object[]{protover}));
        }

      /* Read the length of the variable header */
        if (null == tbuf) {
            tbuf = new byte[4];
            len[1] = 0;
        }
        if (len[1] < 4) {
            try {
                rv = in.read(tbuf, len[1], 4 - len[1]);
            } catch (SocketTimeoutException STe) {
                return null;
            }
            if (-1 == rv) throw new EOFException(getString("transportReturnedEOF"));
            len[1] += rv;
        }
        if (len[1] < 4) {
            if (Debug.debug) Debug.print(Debug.DEBUG, "Only got " + len[1] + " of 4 bytes of header");
            return null;
        }

      /* Parse the variable header length */
        int headerlen = 0;
        if (null == header) {
            headerlen = (int) Message.demarshallint(tbuf, 0, endian, 4);
            if (0 != headerlen % 8)
                headerlen += 8 - (headerlen % 8);
        } else
            headerlen = header.length - 8;

      /* Read the variable header */
        if (null == header) {
            header = new byte[headerlen + 8];
            System.arraycopy(tbuf, 0, header, 0, 4);
            len[2] = 0;
        }
        if (len[2] < headerlen) {
            try {
                rv = in.read(header, 8 + len[2], headerlen - len[2]);
            } catch (SocketTimeoutException STe) {
                return null;
            }
            if (-1 == rv) throw new EOFException(getString("transportReturnedEOF"));
            len[2] += rv;
        }
        if (len[2] < headerlen) {
            if (Debug.debug) Debug.print(Debug.DEBUG, "Only got " + len[2] + " of " + headerlen + " bytes of header");
            return null;
        }

      /* Read the body */
        int bodylen = 0;
        if (null == body) bodylen = (int) Message.demarshallint(buf, 4, endian, 4);
        if (null == body) {
            body = new byte[bodylen];
            len[3] = 0;
        }
        if (len[3] < body.length) {
            try {
                rv = in.read(body, len[3], body.length - len[3]);
            } catch (SocketTimeoutException STe) {
                return null;
            }
            if (-1 == rv) throw new EOFException(getString("transportReturnedEOF"));
            len[3] += rv;
        }
        if (len[3] < body.length) {
            if (Debug.debug) Debug.print(Debug.DEBUG, "Only got " + len[3] + " of " + body.length + " bytes of body");
            return null;
        }

        Message m;
        switch (type) {
            case Message.MessageType.METHOD_CALL:
                m = new MethodCall();
                break;
            case Message.MessageType.METHOD_RETURN:
                m = new MethodReturn();
                break;
            case Message.MessageType.SIGNAL:
                m = new DBusSignal();
                break;
            case Message.MessageType.ERROR:
                m = new Error();
                break;
            default:
                throw new MessageTypeException(MessageFormat.format(getString("messageTypeUnsupported"), new Object[]{type}));
        }
        if (Debug.debug) {
            Debug.print(Debug.VERBOSE, Hexdump.format(buf));
            Debug.print(Debug.VERBOSE, Hexdump.format(tbuf));
            Debug.print(Debug.VERBOSE, Hexdump.format(header));
            Debug.print(Debug.VERBOSE, Hexdump.format(body));
        }
        try {
            m.populate(buf, header, body);
        } catch (DBusException DBe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
            buf = null;
            tbuf = null;
            body = null;
            header = null;
            throw DBe;
        } catch (RuntimeException Re) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, Re);
            buf = null;
            tbuf = null;
            body = null;
            header = null;
            throw Re;
        }
        if (Debug.debug) {
            Debug.print(Debug.INFO, "=> " + m);
        }
        buf = null;
        tbuf = null;
        body = null;
        header = null;
        return m;
    }

    public void close() throws IOException {
        if (Debug.debug) Debug.print(Debug.INFO, "Closing Message Reader");
        in.close();
    }
}
