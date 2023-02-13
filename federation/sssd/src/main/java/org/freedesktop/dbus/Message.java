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
import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.exceptions.UnknownTypeCodeException;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.freedesktop.dbus.Gettext.getString;

/**
 * Superclass of all messages which are sent over the Bus.
 * This class deals with all the marshalling to/from the wire format.
 */
public class Message {
    /**
     * Defines constants representing the endianness of the message.
     */
    public static interface Endian {
        public static final byte BIG = 'B';
        public static final byte LITTLE = 'l';
    }

    /**
     * Defines constants representing the flags which can be set on a message.
     */
    public static interface Flags {
        public static final byte NO_REPLY_EXPECTED = 0x01;
        public static final byte NO_AUTO_START = 0x02;
        public static final byte ASYNC = 0x40;
    }

    /**
     * Defines constants for each message type.
     */
    public static interface MessageType {
        public static final byte METHOD_CALL = 1;
        public static final byte METHOD_RETURN = 2;
        public static final byte ERROR = 3;
        public static final byte SIGNAL = 4;
    }

    /**
     * The current protocol major version.
     */
    public static final byte PROTOCOL = 1;

    /**
     * Defines constants for each valid header field type.
     */
    public static interface HeaderField {
        public static final byte PATH = 1;
        public static final byte INTERFACE = 2;
        public static final byte MEMBER = 3;
        public static final byte ERROR_NAME = 4;
        public static final byte REPLY_SERIAL = 5;
        public static final byte DESTINATION = 6;
        public static final byte SENDER = 7;
        public static final byte SIGNATURE = 8;
    }

    /**
     * Defines constants for each argument type.
     * There are two constants for each argument type,
     * as a byte or as a String (the _STRING version)
     */
    public static interface ArgumentType {
        public static final String BYTE_STRING = "y";
        public static final String BOOLEAN_STRING = "b";
        public static final String INT16_STRING = "n";
        public static final String UINT16_STRING = "q";
        public static final String INT32_STRING = "i";
        public static final String UINT32_STRING = "u";
        public static final String INT64_STRING = "x";
        public static final String UINT64_STRING = "t";
        public static final String DOUBLE_STRING = "d";
        public static final String FLOAT_STRING = "f";
        public static final String STRING_STRING = "s";
        public static final String OBJECT_PATH_STRING = "o";
        public static final String SIGNATURE_STRING = "g";
        public static final String ARRAY_STRING = "a";
        public static final String VARIANT_STRING = "v";
        public static final String STRUCT_STRING = "r";
        public static final String STRUCT1_STRING = "(";
        public static final String STRUCT2_STRING = ")";
        public static final String DICT_ENTRY_STRING = "e";
        public static final String DICT_ENTRY1_STRING = "{";
        public static final String DICT_ENTRY2_STRING = "}";

        public static final byte BYTE = 'y';
        public static final byte BOOLEAN = 'b';
        public static final byte INT16 = 'n';
        public static final byte UINT16 = 'q';
        public static final byte INT32 = 'i';
        public static final byte UINT32 = 'u';
        public static final byte INT64 = 'x';
        public static final byte UINT64 = 't';
        public static final byte DOUBLE = 'd';
        public static final byte FLOAT = 'f';
        public static final byte STRING = 's';
        public static final byte OBJECT_PATH = 'o';
        public static final byte SIGNATURE = 'g';
        public static final byte ARRAY = 'a';
        public static final byte VARIANT = 'v';
        public static final byte STRUCT = 'r';
        public static final byte STRUCT1 = '(';
        public static final byte STRUCT2 = ')';
        public static final byte DICT_ENTRY = 'e';
        public static final byte DICT_ENTRY1 = '{';
        public static final byte DICT_ENTRY2 = '}';
    }

    /**
     * Keep a static reference to each size of padding array to prevent allocation.
     */
    private static byte[][] padding;

    static {
        padding = new byte[][]{
                null,
                new byte[1],
                new byte[2],
                new byte[3],
                new byte[4],
                new byte[5],
                new byte[6],
                new byte[7]};
    }

    /**
     * Steps to increment the buffer array.
     */
    private static final int BUFFERINCREMENT = 20;

    private boolean big;
    protected byte[][] wiredata;
    protected long bytecounter;
    protected Map<Byte, Object> headers;
    protected static long globalserial = 0;
    protected long serial;
    protected byte type;
    protected byte flags;
    protected byte protover;
    private Object[] args;
    private byte[] body;
    private long bodylen = 0;
    private int preallocated = 0;
    private int paofs = 0;
    private byte[] pabuf;
    private int bufferuse = 0;

    /**
     * Returns the name of the given header field.
     */
    public static String getHeaderFieldName(byte field) {
        switch (field) {
            case HeaderField.PATH:
                return "Path";
            case HeaderField.INTERFACE:
                return "Interface";
            case HeaderField.MEMBER:
                return "Member";
            case HeaderField.ERROR_NAME:
                return "Error Name";
            case HeaderField.REPLY_SERIAL:
                return "Reply Serial";
            case HeaderField.DESTINATION:
                return "Destination";
            case HeaderField.SENDER:
                return "Sender";
            case HeaderField.SIGNATURE:
                return "Signature";
            default:
                return "Invalid";
        }
    }

    /**
     * Create a message; only to be called by sub-classes.
     *
     * @param endian The endianness to create the message.
     * @param type   The message type.
     * @param flags  Any message flags.
     */
    protected Message(byte endian, byte type, byte flags) throws DBusException {
        wiredata = new byte[BUFFERINCREMENT][];
        headers = new HashMap<Byte, Object>();
        big = (Endian.BIG == endian);
        bytecounter = 0;
        synchronized (Message.class) {
            serial = ++globalserial;
        }
        if (Debug.debug) Debug.print(Debug.DEBUG, "Creating message with serial " + serial);
        this.type = type;
        this.flags = flags;
        preallocate(4);
        append("yyyy", endian, type, flags, Message.PROTOCOL);
    }

    /**
     * Create a blank message. Only to be used when calling populate.
     */
    protected Message() {
        wiredata = new byte[BUFFERINCREMENT][];
        headers = new HashMap<Byte, Object>();
        bytecounter = 0;
    }

    /**
     * Create a message from wire-format data.
     *
     * @param msg     D-Bus serialized data of type yyyuu
     * @param headers D-Bus serialized data of type a(yv)
     * @param body    D-Bus serialized data of the signature defined in headers.
     */
    @SuppressWarnings("unchecked")
    void populate(byte[] msg, byte[] headers, byte[] body) throws DBusException {
        big = (msg[0] == Endian.BIG);
        type = msg[1];
        flags = msg[2];
        protover = msg[3];
        wiredata[0] = msg;
        wiredata[1] = headers;
        wiredata[2] = body;
        this.body = body;
        bufferuse = 3;
        bodylen = ((Number) extract(Message.ArgumentType.UINT32_STRING, msg, 4)[0]).longValue();
        serial = ((Number) extract(Message.ArgumentType.UINT32_STRING, msg, 8)[0]).longValue();
        bytecounter = msg.length + headers.length + body.length;
        if (Debug.debug) Debug.print(Debug.VERBOSE, headers);
        Object[] hs = extract("a(yv)", headers, 0);
        if (Debug.debug) Debug.print(Debug.VERBOSE, Arrays.deepToString(hs));
        for (Object o : (Vector<Object>) hs[0]) {
            this.headers.put((Byte) ((Object[]) o)[0], ((Variant<Object>) ((Object[]) o)[1]).getValue());
        }
    }

    /**
     * Create a buffer of num bytes.
     * Data is copied to this rather than added to the buffer list.
     */
    private void preallocate(int num) {
        preallocated = 0;
        pabuf = new byte[num];
        appendBytes(pabuf);
        preallocated = num;
        paofs = 0;
    }

    /**
     * Ensures there are enough free buffers.
     *
     * @param num number of free buffers to create.
     */
    private void ensureBuffers(int num) {
        int increase = num - wiredata.length + bufferuse;
        if (increase > 0) {
            if (increase < BUFFERINCREMENT) increase = BUFFERINCREMENT;
            if (Debug.debug) Debug.print(Debug.VERBOSE, "Resizing " + bufferuse);
            byte[][] temp = new byte[wiredata.length + increase][];
            System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
            wiredata = temp;
        }
    }

    /**
     * Appends a buffer to the buffer list.
     */
    protected void appendBytes(byte[] buf) {
        if (null == buf) return;
        if (preallocated > 0) {
            if (paofs + buf.length > pabuf.length)
                throw new ArrayIndexOutOfBoundsException(MessageFormat.format(getString("arrayOutOfBounds"), new Object[]{paofs, pabuf.length, buf.length}));
            System.arraycopy(buf, 0, pabuf, paofs, buf.length);
            paofs += buf.length;
            preallocated -= buf.length;
        } else {
            if (bufferuse == wiredata.length) {
                if (Debug.debug) Debug.print(Debug.VERBOSE, "Resizing " + bufferuse);
                byte[][] temp = new byte[wiredata.length + BUFFERINCREMENT][];
                System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
                wiredata = temp;
            }
            wiredata[bufferuse++] = buf;
            bytecounter += buf.length;
        }
    }

    /**
     * Appends a byte to the buffer list.
     */
    protected void appendByte(byte b) {
        if (preallocated > 0) {
            pabuf[paofs++] = b;
            preallocated--;
        } else {
            if (bufferuse == wiredata.length) {
                if (Debug.debug) Debug.print(Debug.VERBOSE, "Resizing " + bufferuse);
                byte[][] temp = new byte[wiredata.length + BUFFERINCREMENT][];
                System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
                wiredata = temp;
            }
            wiredata[bufferuse++] = new byte[]{b};
            bytecounter++;
        }
    }

    /**
     * Demarshalls an integer of a given width from a buffer.
     * Endianness is determined from the format of the message.
     *
     * @param buf   The buffer to demarshall from.
     * @param ofs   The offset to demarshall from.
     * @param width The byte-width of the int.
     */
    public long demarshallint(byte[] buf, int ofs, int width) {
        return big ? demarshallintBig(buf, ofs, width) : demarshallintLittle(buf, ofs, width);
    }

    /**
     * Demarshalls an integer of a given width from a buffer.
     *
     * @param buf    The buffer to demarshall from.
     * @param ofs    The offset to demarshall from.
     * @param endian The endianness to use in demarshalling.
     * @param width  The byte-width of the int.
     */
    public static long demarshallint(byte[] buf, int ofs, byte endian, int width) {
        return endian == Endian.BIG ? demarshallintBig(buf, ofs, width) : demarshallintLittle(buf, ofs, width);
    }

    /**
     * Demarshalls an integer of a given width from a buffer using big-endian format.
     *
     * @param buf   The buffer to demarshall from.
     * @param ofs   The offset to demarshall from.
     * @param width The byte-width of the int.
     */
    public static long demarshallintBig(byte[] buf, int ofs, int width) {
        long l = 0;
        for (int i = 0; i < width; i++) {
            l <<= 8;
            l |= (buf[ofs + i] & 0xFF);
        }
        return l;
    }

    /**
     * Demarshalls an integer of a given width from a buffer using little-endian format.
     *
     * @param buf   The buffer to demarshall from.
     * @param ofs   The offset to demarshall from.
     * @param width The byte-width of the int.
     */
    public static long demarshallintLittle(byte[] buf, int ofs, int width) {
        long l = 0;
        for (int i = (width - 1); i >= 0; i--) {
            l <<= 8;
            l |= (buf[ofs + i] & 0xFF);
        }
        return l;
    }

    /**
     * Marshalls an integer of a given width and appends it to the message.
     * Endianness is determined from the message.
     *
     * @param l     The integer to marshall.
     * @param width The byte-width of the int.
     */
    public void appendint(long l, int width) {
        byte[] buf = new byte[width];
        marshallint(l, buf, 0, width);
        appendBytes(buf);
    }

    /**
     * Marshalls an integer of a given width into a buffer.
     * Endianness is determined from the message.
     *
     * @param l     The integer to marshall.
     * @param buf   The buffer to marshall to.
     * @param ofs   The offset to marshall to.
     * @param width The byte-width of the int.
     */
    public void marshallint(long l, byte[] buf, int ofs, int width) {
        if (big) marshallintBig(l, buf, ofs, width);
        else marshallintLittle(l, buf, ofs, width);
        if (Debug.debug) Debug.print(Debug.VERBOSE, "Marshalled int " + l + " to " + Hexdump.toHex(buf, ofs, width));
    }

    /**
     * Marshalls an integer of a given width into a buffer using big-endian format.
     *
     * @param l     The integer to marshall.
     * @param buf   The buffer to marshall to.
     * @param ofs   The offset to marshall to.
     * @param width The byte-width of the int.
     */
    public static void marshallintBig(long l, byte[] buf, int ofs, int width) {
        for (int i = (width - 1); i >= 0; i--) {
            buf[i + ofs] = (byte) (l & 0xFF);
            l >>= 8;
        }
    }

    /**
     * Marshalls an integer of a given width into a buffer using little-endian format.
     *
     * @param l     The integer to marshall.
     * @param buf   The buffer to demarshall to.
     * @param ofs   The offset to demarshall to.
     * @param width The byte-width of the int.
     */
    public static void marshallintLittle(long l, byte[] buf, int ofs, int width) {
        for (int i = 0; i < width; i++) {
            buf[i + ofs] = (byte) (l & 0xFF);
            l >>= 8;
        }
    }

    public byte[][] getWireData() {
        return wiredata;
    }

    /**
     * Formats the message in a human-readable format.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append(flags);
        sb.append(',');
        sb.append(serial);
        sb.append(')');
        sb.append(' ');
        sb.append('{');
        sb.append(' ');
        if (headers.size() == 0)
            sb.append('}');
        else {
            for (Byte field : headers.keySet()) {
                sb.append(getHeaderFieldName(field));
                sb.append('=');
                sb.append('>');
                sb.append(headers.get(field).toString());
                sb.append(',');
                sb.append(' ');
            }
            sb.setCharAt(sb.length() - 2, ' ');
            sb.setCharAt(sb.length() - 1, '}');
        }
        sb.append(' ');
        sb.append('{');
        sb.append(' ');
        Object[] args = null;
        try {
            args = getParameters();
        } catch (DBusException DBe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
        }
        if (null == args || 0 == args.length)
            sb.append('}');
        else {
            for (Object o : args) {
                if (o instanceof Object[])
                    sb.append(Arrays.deepToString((Object[]) o));
                else if (o instanceof byte[])
                    sb.append(Arrays.toString((byte[]) o));
                else if (o instanceof int[])
                    sb.append(Arrays.toString((int[]) o));
                else if (o instanceof short[])
                    sb.append(Arrays.toString((short[]) o));
                else if (o instanceof long[])
                    sb.append(Arrays.toString((long[]) o));
                else if (o instanceof boolean[])
                    sb.append(Arrays.toString((boolean[]) o));
                else if (o instanceof double[])
                    sb.append(Arrays.toString((double[]) o));
                else if (o instanceof float[])
                    sb.append(Arrays.toString((float[]) o));
                else
                    sb.append(o.toString());
                sb.append(',');
                sb.append(' ');
            }
            sb.setCharAt(sb.length() - 2, ' ');
            sb.setCharAt(sb.length() - 1, '}');
        }
        return sb.toString();
    }

    /**
     * Returns the value of the header field of a given field.
     *
     * @param type The field to return.
     * @return The value of the field or null if unset.
     */
    public Object getHeader(byte type) {
        return headers.get(type);
    }

    /**
     * Appends a value to the message.
     * The type of the value is read from a D-Bus signature and used to marshall
     * the value.
     *
     * @param sigb   A buffer of the D-Bus signature.
     * @param sigofs The offset into the signature corresponding to this value.
     * @param data   The value to marshall.
     * @return The offset into the signature of the end of this value's type.
     */
    @SuppressWarnings("unchecked")
    private int appendone(byte[] sigb, int sigofs, Object data) throws DBusException {
        try {
            int i = sigofs;
            if (Debug.debug) Debug.print(Debug.VERBOSE, (Object) bytecounter);
            if (Debug.debug) Debug.print(Debug.VERBOSE, "Appending type: " + ((char) sigb[i]) + " value: " + data);

            // pad to the alignment of this type.
            pad(sigb[i]);
            switch (sigb[i]) {
                case ArgumentType.BYTE:
                    appendByte(((Number) data).byteValue());
                    break;
                case ArgumentType.BOOLEAN:
                    appendint(((Boolean) data).booleanValue() ? 1 : 0, 4);
                    break;
                case ArgumentType.DOUBLE:
                    long l = Double.doubleToLongBits(((Number) data).doubleValue());
                    appendint(l, 8);
                    break;
                case ArgumentType.FLOAT:
                    int rf = Float.floatToIntBits(((Number) data).floatValue());
                    appendint(rf, 4);
                    break;
                case ArgumentType.UINT32:
                    appendint(((Number) data).longValue(), 4);
                    break;
                case ArgumentType.INT64:
                    appendint(((Number) data).longValue(), 8);
                    break;
                case ArgumentType.UINT64:
                    if (big) {
                        appendint(((UInt64) data).top(), 4);
                        appendint(((UInt64) data).bottom(), 4);
                    } else {
                        appendint(((UInt64) data).bottom(), 4);
                        appendint(((UInt64) data).top(), 4);
                    }
                    break;
                case ArgumentType.INT32:
                    appendint(((Number) data).intValue(), 4);
                    break;
                case ArgumentType.UINT16:
                    appendint(((Number) data).intValue(), 2);
                    break;
                case ArgumentType.INT16:
                    appendint(((Number) data).shortValue(), 2);
                    break;
                case ArgumentType.STRING:
                case ArgumentType.OBJECT_PATH:
                    // Strings are marshalled as a UInt32 with the length,
                    // followed by the String, followed by a null byte.
                    String payload = data.toString();
                    byte[] payloadbytes = null;
                    try {
                        payloadbytes = payload.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException UEe) {
                        if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(UEe);
                        throw new DBusException(getString("utf8NotSupported"));
                    }
                    if (Debug.debug) Debug.print(Debug.VERBOSE, "Appending String of length " + payloadbytes.length);
                    appendint(payloadbytes.length, 4);
                    appendBytes(payloadbytes);
                    appendBytes(padding[1]);
                    //pad(ArgumentType.STRING);? do we need this?
                    break;
                case ArgumentType.SIGNATURE:
                    // Signatures are marshalled as a byte with the length,
                    // followed by the String, followed by a null byte.
                    // Signatures are generally short, so preallocate the array
                    // for the string, length and null byte.
                    if (data instanceof Type[])
                        payload = Marshalling.getDBusType((Type[]) data);
                    else
                        payload = (String) data;
                    byte[] pbytes = payload.getBytes();
                    preallocate(2 + pbytes.length);
                    appendByte((byte) pbytes.length);
                    appendBytes(pbytes);
                    appendByte((byte) 0);
                    break;
                case ArgumentType.ARRAY:
                    // Arrays are given as a UInt32 for the length in bytes,
                    // padding to the element alignment, then elements in
                    // order. The length is the length from the end of the
                    // initial padding to the end of the last element.
                    if (Debug.debug) {
                        if (data instanceof Object[])
                            Debug.print(Debug.VERBOSE, "Appending array: " + Arrays.deepToString((Object[]) data));
                    }

                    byte[] alen = new byte[4];
                    appendBytes(alen);
                    pad(sigb[++i]);
                    long c = bytecounter;

                    // optimise primatives
                    if (data.getClass().isArray() &&
                            data.getClass().getComponentType().isPrimitive()) {
                        byte[] primbuf;
                        int algn = getAlignment(sigb[i]);
                        int len = Array.getLength(data);
                        switch (sigb[i]) {
                            case ArgumentType.BYTE:
                                primbuf = (byte[]) data;
                                break;
                            case ArgumentType.INT16:
                            case ArgumentType.INT32:
                            case ArgumentType.INT64:
                                primbuf = new byte[len * algn];
                                for (int j = 0, k = 0; j < len; j++, k += algn)
                                    marshallint(Array.getLong(data, j), primbuf, k, algn);
                                break;
                            case ArgumentType.BOOLEAN:
                                primbuf = new byte[len * algn];
                                for (int j = 0, k = 0; j < len; j++, k += algn)
                                    marshallint(Array.getBoolean(data, j) ? 1 : 0, primbuf, k, algn);
                                break;
                            case ArgumentType.DOUBLE:
                                primbuf = new byte[len * algn];
                                if (data instanceof float[])
                                    for (int j = 0, k = 0; j < len; j++, k += algn)
                                        marshallint(Double.doubleToRawLongBits(((float[]) data)[j]),
                                                primbuf, k, algn);
                                else
                                    for (int j = 0, k = 0; j < len; j++, k += algn)
                                        marshallint(Double.doubleToRawLongBits(((double[]) data)[j]),
                                                primbuf, k, algn);
                                break;
                            case ArgumentType.FLOAT:
                                primbuf = new byte[len * algn];
                                for (int j = 0, k = 0; j < len; j++, k += algn)
                                    marshallint(
                                            Float.floatToRawIntBits(((float[]) data)[j]),
                                            primbuf, k, algn);
                                break;
                            default:
                                throw new MarshallingException(getString("arraySentAsNonPrimitive"));
                        }
                        appendBytes(primbuf);
                    } else if (data instanceof List) {
                        Object[] contents = ((List) data).toArray();
                        int diff = i;
                        ensureBuffers(contents.length * 4);
                        for (Object o : contents)
                            diff = appendone(sigb, i, o);
                        i = diff;
                    } else if (data instanceof Map) {
                        int diff = i;
                        ensureBuffers(((Map) data).size() * 6);
                        for (Map.Entry<Object, Object> o : ((Map<Object, Object>) data).entrySet())
                            diff = appendone(sigb, i, o);
                        if (i == diff) {
                            // advance the type parser even on 0-size arrays.
                            Vector<Type> temp = new Vector<Type>();
                            byte[] temp2 = new byte[sigb.length - diff];
                            System.arraycopy(sigb, diff, temp2, 0, temp2.length);
                            String temp3 = new String(temp2);
                            int temp4 = Marshalling.getJavaType(temp3, temp, 1);
                            diff += temp4;
                        }
                        i = diff;
                    } else {
                        Object[] contents = (Object[]) data;
                        ensureBuffers(contents.length * 4);
                        int diff = i;
                        for (Object o : contents)
                            diff = appendone(sigb, i, o);
                        i = diff;
                    }
                    if (Debug.debug)
                        Debug.print(Debug.VERBOSE, "start: " + c + " end: " + bytecounter + " length: " + (bytecounter - c));
                    marshallint(bytecounter - c, alen, 0, 4);
                    break;
                case ArgumentType.STRUCT1:
                    // Structs are aligned to 8 bytes
                    // and simply contain each element marshalled in order
                    Object[] contents;
                    if (data instanceof Container)
                        contents = ((Container) data).getParameters();
                    else
                        contents = (Object[]) data;
                    ensureBuffers(contents.length * 4);
                    int j = 0;
                    for (i++; sigb[i] != ArgumentType.STRUCT2; i++)
                        i = appendone(sigb, i, contents[j++]);
                    break;
                case ArgumentType.DICT_ENTRY1:
                    // Dict entries are the same as structs.
                    if (data instanceof Map.Entry) {
                        i++;
                        i = appendone(sigb, i, ((Map.Entry) data).getKey());
                        i++;
                        i = appendone(sigb, i, ((Map.Entry) data).getValue());
                        i++;
                    } else {
                        contents = (Object[]) data;
                        j = 0;
                        for (i++; sigb[i] != ArgumentType.DICT_ENTRY2; i++)
                            i = appendone(sigb, i, contents[j++]);
                    }
                    break;
                case ArgumentType.VARIANT:
                    // Variants are marshalled as a signature
                    // followed by the value.
                    if (data instanceof Variant) {
                        Variant var = (Variant) data;
                        appendone(new byte[]{ArgumentType.SIGNATURE}, 0, var.getSig());
                        appendone((var.getSig()).getBytes(), 0, var.getValue());
                    } else if (data instanceof Object[]) {
                        contents = (Object[]) data;
                        appendone(new byte[]{ArgumentType.SIGNATURE}, 0, contents[0]);
                        appendone(((String) contents[0]).getBytes(), 0, contents[1]);
                    } else {
                        String sig = Marshalling.getDBusType(data.getClass())[0];
                        appendone(new byte[]{ArgumentType.SIGNATURE}, 0, sig);
                        appendone((sig).getBytes(), 0, data);
                    }
                    break;
            }
            return i;
        } catch (ClassCastException CCe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, CCe);
            throw new MarshallingException(MessageFormat.format(getString("unconvertableType"), new Object[]{data.getClass().getName(), sigb[sigofs]}));
        }
    }

    /**
     * Pad the message to the proper alignment for the given type.
     */
    public void pad(byte type) {
        if (Debug.debug) Debug.print(Debug.VERBOSE, "padding for " + (char) type);
        int a = getAlignment(type);
        if (Debug.debug) Debug.print(Debug.VERBOSE, preallocated + " " + paofs + " " + bytecounter + " " + a);
        int b = (int) ((bytecounter - preallocated) % a);
        if (0 == b) return;
        a = (a - b);
        if (preallocated > 0) {
            paofs += a;
            preallocated -= a;
        } else
            appendBytes(padding[a]);
        if (Debug.debug) Debug.print(Debug.VERBOSE, preallocated + " " + paofs + " " + bytecounter + " " + a);
    }

    /**
     * Return the alignment for a given type.
     */
    public static int getAlignment(byte type) {
        switch (type) {
            case 2:
            case ArgumentType.INT16:
            case ArgumentType.UINT16:
                return 2;
            case 4:
            case ArgumentType.BOOLEAN:
            case ArgumentType.FLOAT:
            case ArgumentType.INT32:
            case ArgumentType.UINT32:
            case ArgumentType.STRING:
            case ArgumentType.OBJECT_PATH:
            case ArgumentType.ARRAY:
                return 4;
            case 8:
            case ArgumentType.INT64:
            case ArgumentType.UINT64:
            case ArgumentType.DOUBLE:
            case ArgumentType.STRUCT:
            case ArgumentType.DICT_ENTRY:
            case ArgumentType.STRUCT1:
            case ArgumentType.DICT_ENTRY1:
            case ArgumentType.STRUCT2:
            case ArgumentType.DICT_ENTRY2:
                return 8;
            case 1:
            case ArgumentType.BYTE:
            case ArgumentType.SIGNATURE:
            case ArgumentType.VARIANT:
            default:
                return 1;
        }
    }

    /**
     * Append a series of values to the message.
     *
     * @param sig  The signature(s) of the value(s).
     * @param data The value(s).
     */
    public void append(String sig, Object... data) throws DBusException {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Appending sig: " + sig + " data: " + Arrays.deepToString(data));
        byte[] sigb = sig.getBytes();
        int j = 0;
        for (int i = 0; i < sigb.length; i++) {
            if (Debug.debug) Debug.print(Debug.VERBOSE, "Appending item: " + i + " " + ((char) sigb[i]) + " " + j);
            i = appendone(sigb, i, data[j++]);
        }
    }

    /**
     * Align a counter to the given type.
     *
     * @param current The current counter.
     * @param type    The type to align to.
     * @return The new, aligned, counter.
     */
    public int align(int current, byte type) {
        if (Debug.debug) Debug.print(Debug.VERBOSE, "aligning to " + (char) type);
        int a = getAlignment(type);
        if (0 == (current % a)) return current;
        return current + (a - (current % a));
    }

    /**
     * Demarshall one value from a buffer.
     *
     * @param sigb      A buffer of the D-Bus signature.
     * @param buf       The buffer to demarshall from.
     * @param ofs       An array of two ints, the offset into the signature buffer
     *                  and the offset into the data buffer. These values will be
     *                  updated to the start of the next value ofter demarshalling.
     * @param contained converts nested arrays to Lists
     * @return The demarshalled value.
     */
    private Object extractone(byte[] sigb, byte[] buf, int[] ofs, boolean contained) throws DBusException {
        if (Debug.debug)
            Debug.print(Debug.VERBOSE, "Extracting type: " + ((char) sigb[ofs[0]]) + " from offset " + ofs[1]);
        Object rv = null;
        ofs[1] = align(ofs[1], sigb[ofs[0]]);
        switch (sigb[ofs[0]]) {
            case ArgumentType.BYTE:
                rv = buf[ofs[1]++];
                break;
            case ArgumentType.UINT32:
                rv = new UInt32(demarshallint(buf, ofs[1], 4));
                ofs[1] += 4;
                break;
            case ArgumentType.INT32:
                rv = (int) demarshallint(buf, ofs[1], 4);
                ofs[1] += 4;
                break;
            case ArgumentType.INT16:
                rv = (short) demarshallint(buf, ofs[1], 2);
                ofs[1] += 2;
                break;
            case ArgumentType.UINT16:
                rv = new UInt16((int) demarshallint(buf, ofs[1], 2));
                ofs[1] += 2;
                break;
            case ArgumentType.INT64:
                rv = demarshallint(buf, ofs[1], 8);
                ofs[1] += 8;
                break;
            case ArgumentType.UINT64:
                long top;
                long bottom;
                if (big) {
                    top = demarshallint(buf, ofs[1], 4);
                    ofs[1] += 4;
                    bottom = demarshallint(buf, ofs[1], 4);
                } else {
                    bottom = demarshallint(buf, ofs[1], 4);
                    ofs[1] += 4;
                    top = demarshallint(buf, ofs[1], 4);
                }
                rv = new UInt64(top, bottom);
                ofs[1] += 4;
                break;
            case ArgumentType.DOUBLE:
                long l = demarshallint(buf, ofs[1], 8);
                ofs[1] += 8;
                rv = Double.longBitsToDouble(l);
                break;
            case ArgumentType.FLOAT:
                int rf = (int) demarshallint(buf, ofs[1], 4);
                ofs[1] += 4;
                rv = Float.intBitsToFloat(rf);
                break;
            case ArgumentType.BOOLEAN:
                rf = (int) demarshallint(buf, ofs[1], 4);
                ofs[1] += 4;
                rv = (1 == rf) ? Boolean.TRUE : Boolean.FALSE;
                break;
            case ArgumentType.ARRAY:
                long size = demarshallint(buf, ofs[1], 4);
                if (Debug.debug) Debug.print(Debug.VERBOSE, "Reading array of size: " + size);
                ofs[1] += 4;
                byte algn = (byte) getAlignment(sigb[++ofs[0]]);
                ofs[1] = align(ofs[1], sigb[ofs[0]]);
                int length = (int) (size / algn);
                if (length > DBusConnection.MAX_ARRAY_LENGTH)
                    throw new MarshallingException(getString("arrayMustNotExceed") + DBusConnection.MAX_ARRAY_LENGTH);
                // optimise primatives
                switch (sigb[ofs[0]]) {
                    case ArgumentType.BYTE:
                        rv = new byte[length];
                        System.arraycopy(buf, ofs[1], rv, 0, length);
                        ofs[1] += size;
                        break;
                    case ArgumentType.INT16:
                        rv = new short[length];
                        for (int j = 0; j < length; j++, ofs[1] += algn)
                            ((short[]) rv)[j] = (short) demarshallint(buf, ofs[1], algn);
                        break;
                    case ArgumentType.INT32:
                        rv = new int[length];
                        for (int j = 0; j < length; j++, ofs[1] += algn)
                            ((int[]) rv)[j] = (int) demarshallint(buf, ofs[1], algn);
                        break;
                    case ArgumentType.INT64:
                        rv = new long[length];
                        for (int j = 0; j < length; j++, ofs[1] += algn)
                            ((long[]) rv)[j] = demarshallint(buf, ofs[1], algn);
                        break;
                    case ArgumentType.BOOLEAN:
                        rv = new boolean[length];
                        for (int j = 0; j < length; j++, ofs[1] += algn)
                            ((boolean[]) rv)[j] = (1 == demarshallint(buf, ofs[1], algn));
                        break;
                    case ArgumentType.FLOAT:
                        rv = new float[length];
                        for (int j = 0; j < length; j++, ofs[1] += algn)
                            ((float[]) rv)[j] =
                                    Float.intBitsToFloat((int) demarshallint(buf, ofs[1], algn));
                        break;
                    case ArgumentType.DOUBLE:
                        rv = new double[length];
                        for (int j = 0; j < length; j++, ofs[1] += algn)
                            ((double[]) rv)[j] =
                                    Double.longBitsToDouble(demarshallint(buf, ofs[1], algn));
                        break;
                    case ArgumentType.DICT_ENTRY1:
                        if (0 == size) {
                            // advance the type parser even on 0-size arrays.
                            Vector<Type> temp = new Vector<Type>();
                            byte[] temp2 = new byte[sigb.length - ofs[0]];
                            System.arraycopy(sigb, ofs[0], temp2, 0, temp2.length);
                            String temp3 = new String(temp2);
                            // ofs[0] gets incremented anyway. Leave one character on the stack
                            int temp4 = Marshalling.getJavaType(temp3, temp, 1) - 1;
                            ofs[0] += temp4;
                            if (Debug.debug)
                                Debug.print(Debug.VERBOSE, "Aligned type: " + temp3 + " " + temp4 + " " + ofs[0]);
                        }
                        int ofssave = ofs[0];
                        long end = ofs[1] + size;
                        Vector<Object[]> entries = new Vector<Object[]>();
                        while (ofs[1] < end) {
                            ofs[0] = ofssave;
                            entries.add((Object[]) extractone(sigb, buf, ofs, true));
                        }
                        rv = new DBusMap<Object, Object>(entries.toArray(new Object[0][]));
                        break;
                    default:
                        if (0 == size) {
                            // advance the type parser even on 0-size arrays.
                            Vector<Type> temp = new Vector<Type>();
                            byte[] temp2 = new byte[sigb.length - ofs[0]];
                            System.arraycopy(sigb, ofs[0], temp2, 0, temp2.length);
                            String temp3 = new String(temp2);
                            // ofs[0] gets incremented anyway. Leave one character on the stack
                            int temp4 = Marshalling.getJavaType(temp3, temp, 1) - 1;
                            ofs[0] += temp4;
                            if (Debug.debug)
                                Debug.print(Debug.VERBOSE, "Aligned type: " + temp3 + " " + temp4 + " " + ofs[0]);
                        }
                        ofssave = ofs[0];
                        end = ofs[1] + size;
                        Vector<Object> contents = new Vector<Object>();
                        while (ofs[1] < end) {
                            ofs[0] = ofssave;
                            contents.add(extractone(sigb, buf, ofs, true));
                        }
                        rv = contents;
                }
                if (contained && !(rv instanceof List) && !(rv instanceof Map))
                    rv = ArrayFrob.listify(rv);
                break;
            case ArgumentType.STRUCT1:
                Vector<Object> contents = new Vector<Object>();
                while (sigb[++ofs[0]] != ArgumentType.STRUCT2)
                    contents.add(extractone(sigb, buf, ofs, true));
                rv = contents.toArray();
                break;
            case ArgumentType.DICT_ENTRY1:
                Object[] decontents = new Object[2];
                if (Debug.debug)
                    Debug.print(Debug.VERBOSE, "Extracting Dict Entry (" + Hexdump.toAscii(sigb, ofs[0], sigb.length - ofs[0]) + ") from: " + Hexdump.toHex(buf, ofs[1], buf.length - ofs[1]));
                ofs[0]++;
                decontents[0] = extractone(sigb, buf, ofs, true);
                ofs[0]++;
                decontents[1] = extractone(sigb, buf, ofs, true);
                ofs[0]++;
                rv = decontents;
                break;
            case ArgumentType.VARIANT:
                int[] newofs = new int[]{0, ofs[1]};
                String sig = (String) extract(ArgumentType.SIGNATURE_STRING, buf, newofs)[0];
                newofs[0] = 0;
                rv = new Variant<Object>(extract(sig, buf, newofs)[0], sig);
                ofs[1] = newofs[1];
                break;
            case ArgumentType.STRING:
                length = (int) demarshallint(buf, ofs[1], 4);
                ofs[1] += 4;
                try {
                    rv = new String(buf, ofs[1], length, "UTF-8");
                } catch (UnsupportedEncodingException UEe) {
                    if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(UEe);
                    throw new DBusException(getString("utf8NotSupported"));
                }
                ofs[1] += length + 1;
                break;
            case ArgumentType.OBJECT_PATH:
                length = (int) demarshallint(buf, ofs[1], 4);
                ofs[1] += 4;
                rv = new ObjectPath(getSource(), new String(buf, ofs[1], length));
                ofs[1] += length + 1;
                break;
            case ArgumentType.SIGNATURE:
                length = (buf[ofs[1]++] & 0xFF);
                rv = new String(buf, ofs[1], length);
                ofs[1] += length + 1;
                break;
            default:
                throw new UnknownTypeCodeException(sigb[ofs[0]]);
        }
        if (Debug.debug) if (rv instanceof Object[])
            Debug.print(Debug.VERBOSE, "Extracted: " + Arrays.deepToString((Object[]) rv) + " (now at " + ofs[1] + ")");
        else
            Debug.print(Debug.VERBOSE, "Extracted: " + rv + " (now at " + ofs[1] + ")");
        return rv;
    }

    /**
     * Demarshall values from a buffer.
     *
     * @param sig The D-Bus signature(s) of the value(s).
     * @param buf The buffer to demarshall from.
     * @param ofs The offset into the data buffer to start.
     * @return The demarshalled value(s).
     */
    public Object[] extract(String sig, byte[] buf, int ofs) throws DBusException {
        return extract(sig, buf, new int[]{0, ofs});
    }

    /**
     * Demarshall values from a buffer.
     *
     * @param sig The D-Bus signature(s) of the value(s).
     * @param buf The buffer to demarshall from.
     * @param ofs An array of two ints, the offset into the signature
     *            and the offset into the data buffer. These values will be
     *            updated to the start of the next value ofter demarshalling.
     * @return The demarshalled value(s).
     */
    public Object[] extract(String sig, byte[] buf, int[] ofs) throws DBusException {
        if (Debug.debug)
            Debug.print(Debug.VERBOSE, "extract(" + sig + ",#" + buf.length + ", {" + ofs[0] + "," + ofs[1] + "}");
        Vector<Object> rv = new Vector<Object>();
        byte[] sigb = sig.getBytes();
        for (int[] i = ofs; i[0] < sigb.length; i[0]++) {
            rv.add(extractone(sigb, buf, i, false));
        }
        return rv.toArray();
    }

    /**
     * Returns the Bus ID that sent the message.
     */
    public String getSource() {
        return (String) headers.get(HeaderField.SENDER);
    }

    /**
     * Returns the destination of the message.
     */
    public String getDestination() {
        return (String) headers.get(HeaderField.DESTINATION);
    }

    /**
     * Returns the interface of the message.
     */
    public String getInterface() {
        return (String) headers.get(HeaderField.INTERFACE);
    }

    /**
     * Returns the object path of the message.
     */
    public String getPath() {
        Object o = headers.get(HeaderField.PATH);
        if (null == o) return null;
        return o.toString();
    }

    /**
     * Returns the member name or error name this message represents.
     */
    public String getName() {
        if (this instanceof Error)
            return (String) headers.get(HeaderField.ERROR_NAME);
        else
            return (String) headers.get(HeaderField.MEMBER);
    }

    /**
     * Returns the dbus signature of the parameters.
     */
    public String getSig() {
        return (String) headers.get(HeaderField.SIGNATURE);
    }

    /**
     * Returns the message flags.
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns the message serial ID (unique for this connection)
     *
     * @return the message serial.
     */
    public long getSerial() {
        return serial;
    }

    /**
     * If this is a reply to a message, this returns its serial.
     *
     * @return The reply serial, or 0 if it is not a reply.
     */
    public long getReplySerial() {
        Number l = (Number) headers.get(HeaderField.REPLY_SERIAL);
        if (null == l) return 0;
        return l.longValue();
    }

    /**
     * Parses and returns the parameters to this message as an Object array.
     */
    public Object[] getParameters() throws DBusException {
        if (null == args && null != body) {
            String sig = (String) headers.get(HeaderField.SIGNATURE);
            if (null != sig && 0 != body.length) {
                args = extract(sig, body, 0);
            } else args = new Object[0];
        }
        return args;
    }

    protected void setArgs(Object[] args) {
        this.args = args;
    }

    /**
     * Warning, do not use this method unless you really know what you are doing.
     */
    public void setSource(String source) throws DBusException {
        if (null != body) {
            wiredata = new byte[BUFFERINCREMENT][];
            bufferuse = 0;
            bytecounter = 0;
            preallocate(12);
            append("yyyyuu", big ? Endian.BIG : Endian.LITTLE, type, flags, protover, bodylen, serial);
            headers.put(HeaderField.SENDER, source);
            Object[][] newhead = new Object[headers.size()][];
            int i = 0;
            for (Byte b : headers.keySet()) {
                newhead[i] = new Object[2];
                newhead[i][0] = b;
                newhead[i][1] = headers.get(b);
                i++;
            }
            append("a(yv)", (Object) newhead);
            pad((byte) 8);
            appendBytes(body);
        }
    }
}
