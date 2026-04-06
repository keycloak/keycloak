package org.freedesktop.dbus.messages;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.freedesktop.dbus.ArrayFrob;
import org.freedesktop.dbus.Container;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.exceptions.UnknownTypeCodeException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.constants.ArgumentType;
import org.freedesktop.dbus.messages.constants.Endian;
import org.freedesktop.dbus.messages.constants.HeaderField;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.Hexdump;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.freedesktop.dbus.utils.PrimitiveUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.freedesktop.dbus.messages.constants.ArgumentType.ARRAY;
import static org.freedesktop.dbus.messages.constants.ArgumentType.BOOLEAN;
import static org.freedesktop.dbus.messages.constants.ArgumentType.BYTE;
import static org.freedesktop.dbus.messages.constants.ArgumentType.DICT_ENTRY;
import static org.freedesktop.dbus.messages.constants.ArgumentType.DICT_ENTRY1;
import static org.freedesktop.dbus.messages.constants.ArgumentType.DICT_ENTRY2;
import static org.freedesktop.dbus.messages.constants.ArgumentType.DOUBLE;
import static org.freedesktop.dbus.messages.constants.ArgumentType.FILEDESCRIPTOR;
import static org.freedesktop.dbus.messages.constants.ArgumentType.FLOAT;
import static org.freedesktop.dbus.messages.constants.ArgumentType.INT16;
import static org.freedesktop.dbus.messages.constants.ArgumentType.INT32;
import static org.freedesktop.dbus.messages.constants.ArgumentType.INT64;
import static org.freedesktop.dbus.messages.constants.ArgumentType.OBJECT_PATH;
import static org.freedesktop.dbus.messages.constants.ArgumentType.SIGNATURE;
import static org.freedesktop.dbus.messages.constants.ArgumentType.SIGNATURE_STRING;
import static org.freedesktop.dbus.messages.constants.ArgumentType.STRING;
import static org.freedesktop.dbus.messages.constants.ArgumentType.STRUCT;
import static org.freedesktop.dbus.messages.constants.ArgumentType.STRUCT1;
import static org.freedesktop.dbus.messages.constants.ArgumentType.STRUCT2;
import static org.freedesktop.dbus.messages.constants.ArgumentType.UINT16;
import static org.freedesktop.dbus.messages.constants.ArgumentType.UINT32;
import static org.freedesktop.dbus.messages.constants.ArgumentType.UINT32_STRING;
import static org.freedesktop.dbus.messages.constants.ArgumentType.UINT64;
import static org.freedesktop.dbus.messages.constants.ArgumentType.VARIANT;


/**
 * Superclass of all messages which are sent over the Bus.<br>
 * This class deals with all the marshalling to/from the wire format.
 */
public class Message {
    public static final int             MAXIMUM_ARRAY_LENGTH   = 67108864;
    public static final int             MAXIMUM_MESSAGE_LENGTH = MAXIMUM_ARRAY_LENGTH * 2;
    public static final int             MAXIMUM_NUM_UNIX_FDS   = MAXIMUM_MESSAGE_LENGTH / 4;

    /** The current protocol major version. */
    public static final byte            PROTOCOL               = 1;

    /** Default extraction options. */
    private static final ExtractOptions DEFAULT_OPTIONS        = new ExtractOptions(false, List.of());

    /** Position of data offset in int array. */
    private static final int            OFFSET_DATA            = 1;
    /** Position of signature offset in int array. */
    private static final int            OFFSET_SIG             = 0;

    /** Keep a static reference to each size of padding array to prevent allocation. */
    private static byte[][]  padding;
    static {
        padding = new byte[][] {
                null, new byte[1], new byte[2], new byte[3], new byte[4], new byte[5], new byte[6], new byte[7]
        };
    }
    /** Steps to increment the buffer array. */
    private static final int           BUFFERINCREMENT = 20;

    private static final AtomicLong    GLOBAL_SERIAL    = new AtomicLong(0);

    //CHECKSTYLE:OFF
    protected final Logger             logger          = LoggerFactory.getLogger(getClass());
    //CHECKSTYLE:ON

    private final List<FileDescriptor> filedescriptors = new ArrayList<>();
    private final Object[]             headers         = new Object[HeaderField.MAX_FIELDS];

    private byte[][]                   wiredata        = new byte[BUFFERINCREMENT][];
    private long                       bytecounter     = 0;

    private long                       serial;
    private byte                       type;
    private byte                       flags;
    private byte                       protover;

    private boolean                    big;
    private Object[]                   args;
    private byte[]                     body;
    private long                       bodylen         = 0;
    private int                        preallocated    = 0;
    private int                        paofs           = 0;
    private byte[]                     pabuf;
    private int                        bufferuse       = 0;

    private boolean                    endianWasSet;

    /**
     * Create a message; only to be called by sub-classes.
     *
     * @param _endian The endianness to create the message.
     * @param _type The message type.
     * @param _flags Any message flags.
     * @throws DBusException on error
     */
    protected Message(byte _endian, byte _type, byte _flags) throws DBusException {
        this();
        big = Endian.BIG == _endian;
        setSerial(GLOBAL_SERIAL.incrementAndGet());

        logger.debug("Creating message with serial {}", getSerial());

        type = _type;
        flags = _flags;
        preallocate(4);
        endianWasSet = _endian != (byte) 0;
        append("yyyy", _endian, _type, _flags, PROTOCOL);
    }

    /**
     * Create a blank message. Only to be used when calling populate.
     */
    protected Message() {
    }

    public void updateEndianess(byte _endianess) {
        if (endianWasSet) {
            return;
        }

        if (wiredata[0] != null) {
            wiredata[0][0] = _endianess;
        } else {
            wiredata[0] = new byte[] {_endianess, 0, 0, 0};
        }

        endianWasSet = true;
    }

    /**
     * Create a message from wire-format data.
     *
     * @param _msg D-Bus serialized data of type yyyuu
     * @param _headers D-Bus serialized data of type a(yv)
     * @param _body D-Bus serialized data of the signature defined in headers.
     */
    @SuppressWarnings("unchecked")
    void populate(byte[] _msg, byte[] _headers, byte[] _body, List<FileDescriptor> _descriptors) throws DBusException {

        // create a copy of the given arrays to be sure that the content is not changed outside

        byte[] msgBuf = new byte[_msg.length];
        System.arraycopy(_msg, 0, msgBuf, 0, _msg.length);

        byte[] headerBuf = new byte[_headers.length];
        System.arraycopy(_headers, 0, headerBuf, 0, _headers.length);

        byte[] bodyBuf = new byte[_body.length];
        System.arraycopy(_body, 0, bodyBuf, 0, _body.length);

        endianWasSet = true;

        big = msgBuf[0] == Endian.BIG;
        type = msgBuf[1];
        flags = msgBuf[2];
        protover = msgBuf[3];
        wiredata[0] = msgBuf;
        wiredata[1] = headerBuf;
        wiredata[2] = bodyBuf;
        body = bodyBuf;
        bufferuse = 3;
        bodylen = ((Number) extract(UINT32_STRING, msgBuf, 4, DEFAULT_OPTIONS)[0]).longValue();

        long extractedSerial = ((Number) extract(UINT32_STRING, msgBuf, 8, DEFAULT_OPTIONS)[0]).longValue();

        logger.debug("Received message of type {} with serial {}", type, extractedSerial);

        setSerial(extractedSerial);

        // cast to ensure everything is upgraded to long
        bytecounter = (long) msgBuf.length + headerBuf.length + bodyBuf.length;

        filedescriptors.clear();
        if (_descriptors != null) {
            filedescriptors.addAll(_descriptors);
        }

        LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("Message header: {}", Hexdump.toAscii(headerBuf)));

        Object[] hs = extractHeader(headerBuf);

        LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("Extracted objects: {}", LoggingHelper.arraysVeryDeepString(hs)));

        List<Object> list = (List<Object>) hs[0];
        for (Object o : list) {
            Object[] objArr = (Object[]) o;
            byte idx = (byte) objArr[0];
            this.headers[idx] = objArr[1];
        }
    }

    protected Object[] getHeader() {
        return headers;
    }

    /**
     * Set header content.
     * <code>null</code> value is ignored.
     *
     * @param _header header to set
     */
    protected void setHeader(Object[] _header) {
        if (_header == null) {
            return;
        }
        if (_header.length > headers.length) {
            throw new IllegalArgumentException("Given header is larger (" + _header.length + ") than allowed header size: " + headers.length);
        }
        System.arraycopy(_header, 0, headers, 0, _header.length);
    }

    protected long getByteCounter() {
        return bytecounter;
    }

    protected void setByteCounter(long _bytecounter) {
        bytecounter = _bytecounter;
    }

    protected synchronized void setSerial(long _serial) {
        serial = _serial;
    }

    protected void setWireData(byte[][] _wiredata) {
        wiredata = _wiredata;
    }

    byte getProtover() {
        return protover;
    }

    long getBodylen() {
        return bodylen;
    }

    /**
     * Create a buffer of num bytes. Data is copied to this rather than added to the buffer list.
     */
    private void preallocate(int _num) {
        preallocated = 0;
        pabuf = new byte[_num];
        appendBytes(pabuf);
        preallocated = _num;
        paofs = 0;
    }

    /**
     * Ensures there are enough free buffers.
     *
     * @param _num number of free buffers to create.
     */
    private void ensureBuffers(int _num) {
        int increase = _num - wiredata.length + bufferuse;
        if (increase > 0) {
            if (increase < BUFFERINCREMENT) {
                increase = BUFFERINCREMENT;
            }

            logger.trace("Resizing {}", bufferuse);

            byte[][] temp = new byte[wiredata.length + increase][];
            System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
            wiredata = temp;
        }
    }

    /**
     * Appends a buffer to the buffer list.
     *
     * @param _buf buffer byte array
     */
    protected void appendBytes(byte[] _buf) {
        if (null == _buf) {
            return;
        }
        if (preallocated > 0) {
            if (paofs + _buf.length > pabuf.length) {
                throw new ArrayIndexOutOfBoundsException(
                        MessageFormat.format("Array index out of bounds, paofs={0}, pabuf.length={1}, buf.length={2}.",
                                paofs, pabuf.length, _buf.length));
            }
            System.arraycopy(_buf, 0, pabuf, paofs, _buf.length);
            paofs += _buf.length;
            preallocated -= _buf.length;
        } else {
            if (bufferuse == wiredata.length) {
                logger.trace("Resizing {}", bufferuse);
                byte[][] temp = new byte[wiredata.length + BUFFERINCREMENT][];
                System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
                wiredata = temp;
            }
            wiredata[bufferuse++] = _buf;
            bytecounter += _buf.length;
        }
    }

    /**
     * Appends a byte to the buffer list.
     *
     * @param _b byte
     */
    protected void appendByte(byte _b) {
        if (preallocated > 0) {
            pabuf[paofs++] = _b;
            preallocated--;
        } else {
            if (bufferuse == wiredata.length) {

                logger.trace("Resizing {}", bufferuse);
                byte[][] temp = new byte[wiredata.length + BUFFERINCREMENT][];
                System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
                wiredata = temp;
            }
            wiredata[bufferuse++] = new byte[] {
                    _b
            };
            bytecounter++;
        }
    }

    /**
     * Demarshalls an integer of a given width from a buffer. Endianness is determined from the format of the message.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _width The byte-width of the int.
     *
     * @return long
     */
    protected long demarshallint(byte[] _buf, int _ofs, int _width) {
        return big ? demarshallintBig(_buf, _ofs, _width) : demarshallintLittle(_buf, _ofs, _width);
    }

    /**
     * Marshalls an integer of a given width and appends it to the message. Endianness is determined from the message.
     *
     * @param _l The integer to marshall.
     * @param _width The byte-width of the int.
     */
    protected void appendint(long _l, int _width) {
        byte[] buf = new byte[_width];
        marshallint(_l, buf, 0, _width);
        appendBytes(buf);
    }

    /**
     * Marshalls an integer of a given width into a buffer. Endianness is determined from the message.
     *
     * @param _l The integer to marshall.
     * @param _buf The buffer to marshall to.
     * @param _ofs The offset to marshall to.
     * @param _width The byte-width of the int.
     */
    protected void marshallint(long _l, byte[] _buf, int _ofs, int _width) {
        if (big) {
            marshallintBig(_l, _buf, _ofs, _width);
        } else {
            marshallintLittle(_l, _buf, _ofs, _width);
        }

        LoggingHelper.logIf(logger.isTraceEnabled(),
                () -> logger.trace("Marshalled int {} to {}", _l, Hexdump.toHex(_buf, _ofs, _width, true)));
    }

    public byte[][] getWireData() {
        return wiredata;
    }

    public List<FileDescriptor> getFiledescriptors() {
        return filedescriptors;
    }

    /**
     * Formats the message in a human-readable format.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append(flags);
        sb.append(',');
        sb.append(getSerial());
        sb.append(')');
        sb.append(' ');
        sb.append('{');
        sb.append(' ');
        if (headers.length == 0) {
            sb.append('}');
        } else {
            for (int i = 0; i < headers.length; i++) {
                sb.append(getHeaderFieldName((byte) i));
                sb.append('=');
                sb.append('>');
                sb.append(headers[i]);
                sb.append(',');
                sb.append(' ');
            }
            sb.setCharAt(sb.length() - 2, ' ');
            sb.setCharAt(sb.length() - 1, '}');
        }
        sb.append(' ');
        sb.append('{');
        sb.append(' ');
        Object[] largs = null;
        try {
            largs = extractArgs(null);
        } catch (DBusException _ex) {
            logger.debug("", _ex);
        }
        if (null == largs || 0 == largs.length) {
            sb.append('}');
        } else {
            for (Object o : largs) {
                if (o == null) {
                    sb.append("null");
                } else if (o instanceof Object[] oa) {
                    sb.append(Arrays.deepToString(oa));
                } else if (o instanceof byte[] ba) {
                    sb.append(Arrays.toString(ba));
                } else if (o instanceof int[] ia) {
                    sb.append(Arrays.toString(ia));
                } else if (o instanceof short[] sa) {
                    sb.append(Arrays.toString(sa));
                } else if (o instanceof long[] la) {
                    sb.append(Arrays.toString(la));
                } else if (o instanceof boolean[] ba) {
                    sb.append(Arrays.toString(ba));
                } else if (o instanceof double[] da) {
                    sb.append(Arrays.toString(da));
                } else if (o instanceof float[] fa) {
                    sb.append(Arrays.toString(fa));
                } else {
                    sb.append(o);
                }
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
     * @param _type The field to return.
     * @return The value of the field or null if unset.
     */
    protected Object getHeader(byte _type) {
        return headers.length == 0 || headers.length < _type ? null : headers[_type];
    }

    /**
     * Pad the message to the proper alignment for the given type.
     *
     * @param _type type
     */
    protected void pad(byte _type) {
        logger.trace("padding for {}", (char) _type);
        int a = getAlignment(_type);
        logger.trace("{} {} {} {}", preallocated, paofs, bytecounter, a);
        int b = (int) ((bytecounter - preallocated) % a);
        if (0 == b) {
            return;
        }
        a = a - b;
        if (preallocated > 0) {
            paofs += a;
            preallocated -= a;
        } else {
            appendBytes(padding[a]);
        }
        logger.trace("{} {} {} {}", preallocated, paofs, bytecounter, a);

    }

    /**
     * Append a series of values to the message.
     *
     * @param _sig The signature(s) of the value(s).
     * @param _data The value(s).
     *
     * @throws DBusException on error
     */
    protected void append(String _sig, Object... _data) throws DBusException {
        LoggingHelper.logIf(logger.isDebugEnabled(), () -> logger.debug("Appending sig: {} data: {}", _sig, LoggingHelper.arraysVeryDeepString(_data)));

        byte[] sigb = _sig.getBytes();
        int j = 0;
        for (int i = 0; i < sigb.length; i++) {
            logger.trace("Appending item: {} {} {}", i, (char) sigb[i], j);
            i = appendOne(sigb, i, _data[j++]);
        }
    }

    /**
     * Appends a value to the message. The type of the value is read from a D-Bus signature and used to marshall the
     * value.
     *
     * @param _sigb A buffer of the D-Bus signature.
     * @param _sigofs The offset into the signature corresponding to this value.
     * @param _data The value to marshall.
     * @return The offset into the signature of the end of this value's type.
     */
    private int appendOne(byte[] _sigb, int _sigofs, Object _data) throws DBusException {
        try {
            int i = _sigofs;
            logger.trace("{}", bytecounter);
            logger.trace("Appending type: {} value: {}", (char) _sigb[i], _data);

            // pad to the alignment of this type.
            pad(_sigb[i]);
            switch (_sigb[i]) {
            case BYTE:
                appendByte(((Number) _data).byteValue());
                break;
            case BOOLEAN:
                appendint((Boolean) _data ? 1 : 0, 4);
                break;
            case DOUBLE:
                long l = Double.doubleToLongBits(((Number) _data).doubleValue());
                appendint(l, 8);
                break;
            case FLOAT:
                int rf = Float.floatToIntBits(((Number) _data).floatValue());
                appendint(rf, 4);
                break;
            case UINT32:
                appendint(((Number) _data).longValue(), 4);
                break;
            case INT64:
                appendint(((Number) _data).longValue(), 8);
                break;
            case UINT64:
                if (big) {
                    appendint(((UInt64) _data).top(), 4);
                    appendint(((UInt64) _data).bottom(), 4);
                } else {
                    appendint(((UInt64) _data).bottom(), 4);
                    appendint(((UInt64) _data).top(), 4);
                }
                break;
            case INT32:
                appendint(((Number) _data).intValue(), 4);
                break;
            case UINT16:
                appendint(((Number) _data).intValue(), 2);
                break;
            case INT16:
                appendint(((Number) _data).shortValue(), 2);
                break;
            case FILEDESCRIPTOR:
                filedescriptors.add((FileDescriptor) _data);
                appendint(filedescriptors.size() - 1L, 4);
                logger.debug("Just inserted {} as filedescriptor", filedescriptors.size() - 1);
                break;
            case STRING, OBJECT_PATH:

                String payload;
                // if the given data is an object, not a ObjectPath itself
                if (_data instanceof DBusInterface di) {
                    payload = di.getObjectPath();
                } else {
                    // Strings are marshalled as a UInt32 with the length,
                    // followed by the String, followed by a null byte.
                    payload = _data.toString();
                }

                byte[] payloadbytes = payload.getBytes(StandardCharsets.UTF_8);
                logger.trace("Appending String of length {}", payloadbytes.length);
                appendint(payloadbytes.length, 4);
                appendBytes(payloadbytes);
                appendBytes(padding[1]);
                // pad(ArgumentType.STRING);? do we need this?
                break;
            case SIGNATURE:
                // Signatures are marshalled as a byte with the length,
                // followed by the String, followed by a null byte.
                // Signatures are generally short, so preallocate the array
                // for the string, length and null byte.
                if (_data instanceof Type[] ta) {
                    payload = Marshalling.getDBusType(ta);
                } else {
                    payload = (String) _data;
                }
                byte[] pbytes = payload.getBytes();
                preallocate(2 + pbytes.length);
                appendByte((byte) pbytes.length);
                appendBytes(pbytes);
                appendByte((byte) 0);
                break;
            case ARRAY:
                // Arrays are given as a UInt32 for the length in bytes,
                // padding to the element alignment, then elements in
                // order. The length is the length from the end of the
                // initial padding to the end of the last element.
                if (logger.isTraceEnabled() && _data instanceof Object[] oa) {
                    logger.trace("Appending array: {}", Arrays.deepToString(oa));
                }

                byte[] alen = new byte[4];
                appendBytes(alen);
                pad(_sigb[++i]);
                long c = bytecounter;

                // optimise primitives
                if (_data.getClass().isArray() && _data.getClass().getComponentType().isPrimitive()) {
                    byte[] primbuf;
                    int algn = getAlignment(_sigb[i]);
                    int len = Array.getLength(_data);
                    switch (_sigb[i]) {
                        case BYTE -> {
                            primbuf = (byte[]) _data;
                        }
                        case INT16, INT32, INT64 -> {
                            primbuf = new byte[len * algn];
                            for (int j = 0, k = 0; j < len; j++, k += algn) {
                                marshallint(Array.getLong(_data, j), primbuf, k, algn);
                            }
                        }
                        case BOOLEAN -> {
                            primbuf = new byte[len * algn];
                            for (int j = 0, k = 0; j < len; j++, k += algn) {
                                marshallint(Array.getBoolean(_data, j) ? 1 : 0, primbuf, k, algn);
                            }
                        }
                        case DOUBLE -> {
                            primbuf = new byte[len * algn];
                            if (_data instanceof float[] fa) {
                                for (int j = 0, k = 0; j < len; j++, k += algn) {
                                    marshallint(Double.doubleToRawLongBits(fa[j]), primbuf, k, algn);
                                }
                            } else {
                                for (int j = 0, k = 0; j < len; j++, k += algn) {
                                    marshallint(Double.doubleToRawLongBits(((double[]) _data)[j]), primbuf, k, algn);
                                }
                            }
                        }
                        case FLOAT -> {
                            primbuf = new byte[len * algn];
                            for (int j = 0, k = 0; j < len; j++, k += algn) {
                                marshallint(Float.floatToRawIntBits(((float[]) _data)[j]), primbuf, k, algn);
                            }
                        }
                        default -> throw new MarshallingException("Primitive array being sent as non-primitive array.");
                    }
                    appendBytes(primbuf);
                } else if (_data instanceof Collection<?> coll) {
                    Object[] contents = coll.toArray();
                    int diff = i;
                    ensureBuffers(contents.length * 4);
                    for (Object o : contents) {
                        diff = appendOne(_sigb, i, o);
                    }
                    if (contents.length == 0) {
                        diff = EmptyCollectionHelper.determineSignatureOffsetArray(_sigb, diff);
                    }
                    i = diff;
                } else if (_data instanceof Map<?, ?> map) {
                    int diff = i;
                    ensureBuffers(map.size() * 6);
                    for (Map.Entry<?, ?> o : map.entrySet()) {
                        diff = appendOne(_sigb, i, o);
                    }
                    if (map.isEmpty()) {
                        diff = EmptyCollectionHelper.determineSignatureOffsetDict(_sigb, diff);
                    }
                    i = diff;
                } else {
                    Object[] contents = (Object[]) _data;
                    ensureBuffers(contents.length * 4);
                    int diff = i;
                    for (Object o : contents) {
                        diff = appendOne(_sigb, i, o);
                    }
                    if (contents.length == 0) {
                        diff = EmptyCollectionHelper.determineSignatureOffsetArray(_sigb, diff);
                    }
                    i = diff;
                }
                logger.trace("start: {} end: {} length: {}", c, bytecounter, bytecounter - c);
                marshallint(bytecounter - c, alen, 0, 4);
                break;
            case STRUCT1:
                // Structs are aligned to 8 bytes
                // and simply contain each element marshalled in order
                Object[] contents;
                if (_data instanceof Container cont) {
                    contents = cont.getParameters();
                } else {
                    contents = (Object[]) _data;
                }
                ensureBuffers(contents.length * 4);
                int j = 0;
                for (i++; _sigb[i] != STRUCT2; i++) {
                    i = appendOne(_sigb, i, contents[j++]);
                }
                break;
            case DICT_ENTRY1:
                // Dict entries are the same as structs.
                if (_data instanceof Map.Entry<?, ?> entry) {
                    i++;
                    i = appendOne(_sigb, i, entry.getKey());
                    i++;
                    i = appendOne(_sigb, i, entry.getValue());
                    i++;
                } else {
                    contents = (Object[]) _data;
                    j = 0;
                    for (i++; _sigb[i] != DICT_ENTRY2; i++) {
                        i = appendOne(_sigb, i, contents[j++]);
                    }
                }
                break;
            case VARIANT:
                // Variants are marshalled as a signature
                // followed by the value.
                if (_data instanceof Variant<?> variant) {
                    appendOne(new byte[] {
                        SIGNATURE
                    }, 0, variant.getSig());
                    appendOne(variant.getSig().getBytes(), 0, variant.getValue());
                } else if (_data instanceof Object[] oa) {
                    appendOne(new byte[] {
                        SIGNATURE
                    }, 0, oa[0]);
                    appendOne(((String) oa[0]).getBytes(), 0, oa[1]);
                } else {
                    String sig = Marshalling.getDBusType(_data.getClass())[0];
                    appendOne(new byte[] {
                        SIGNATURE
                    }, 0, sig);
                    appendOne(sig.getBytes(), 0, _data);
                }
                break;
            default:
            }
            return i;
        } catch (ClassCastException _ex) {
            logger.debug("Trying to marshall to unconvertible type.", _ex);
            throw new MarshallingException(
                    MessageFormat.format("Trying to marshall to unconvertible type (from {0} to {1}).",
                            _data.getClass().getName(), (char) _sigb[_sigofs]));
        }
    }

    /**
     * Align a counter to the given type.
     *
     * @param _current The current counter.
     * @param _type The type to align to.
     * @return The new, aligned, counter.
     */
    protected int align(int _current, byte _type) {
        logger.trace("aligning to {}", (char) _type);
        int a = getAlignment(_type);
        if (0 == _current % a) {
            return _current;
        }
        return _current + a - _current % a;
    }

    /**
     * Extracts the header information from the given byte array.
     *
     * @param _headers D-Bus serialized data of type a(yv)
     *
     * @return Object array containing header data
     *
     * @throws DBusException when parsing fails
     */
    Object[] extractHeader(byte[] _headers) throws DBusException {
        int[] offsets = new int[] {
                0, 0
        };

        return extract("a(yv)", _headers, offsets, DEFAULT_OPTIONS, this::readHeaderVariants);
    }

    /**
     * Special lightweight version to read the variant objects in DBus message header.
     * This method will not create {@link Variant} objects it directly extracts the Variant data content.
     *
     * @param _signatureBuf DBus signature string as byte array
     * @param _dataBuf buffer with header data
     * @param _offsets current offsets
     * @param _options additional options
     *
     * @return Object
     *
     * @throws DBusException when parsing fails
     */
    private Object readHeaderVariants(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, ExtractOptions _options) throws DBusException {
        // correct the offsets before extracting values
        _offsets[OFFSET_DATA] = align(_offsets[OFFSET_DATA], _signatureBuf[_offsets[OFFSET_SIG]]);

        Object result = null;
        if (_signatureBuf[_offsets[OFFSET_SIG]] == ARRAY) {
            result = extractArray(_signatureBuf, _dataBuf, _offsets, _options, this::readHeaderVariants);
        } else if (_signatureBuf[_offsets[OFFSET_SIG]] == BYTE) {
            result = extractByte(_dataBuf, _offsets);
        } else if (_signatureBuf[_offsets[OFFSET_SIG]] == VARIANT) {
            result = extractVariant(_dataBuf, _offsets, DEFAULT_OPTIONS, (sig, obj) -> obj);
        } else if (_signatureBuf[_offsets[OFFSET_SIG]] == STRUCT1) {
            result = extractStruct(_signatureBuf, _dataBuf, _offsets, DEFAULT_OPTIONS, this::readHeaderVariants);
        } else {
            throw new MessageFormatException("Unsupported data type in header: " + _signatureBuf[_offsets[OFFSET_SIG]]);
        }

        logger.trace("Extracted header signature type '{}' to: '{}'", (char) _signatureBuf[_offsets[OFFSET_SIG]], result);

        return result;
    }

    /**
     * Demarshall one value from a buffer.
     *
     * @param _signatureBuf A buffer of the D-Bus signature.
     * @param _dataBuf The buffer to demarshall from.
     * @param _offsets An array of two ints, which holds the position of the current signature offset and the current
     *            offset of the data buffer.
     * @param _options extract options
     * @return The demarshalled value.
     */
    private Object extractOne(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, ExtractOptions _options)
            throws DBusException {

        logger.trace("Extracting type: {} from offset {}", (char) _signatureBuf[_offsets[OFFSET_SIG]],
                _offsets[OFFSET_DATA]);

        Object rv = null;
        _offsets[OFFSET_DATA] = align(_offsets[OFFSET_DATA], _signatureBuf[_offsets[OFFSET_SIG]]);
        switch (_signatureBuf[_offsets[OFFSET_SIG]]) {
            case BYTE:
                rv = extractByte(_dataBuf, _offsets);
                break;
            case UINT32:
                rv = new UInt32(demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4));
                _offsets[OFFSET_DATA] += 4;
                break;
            case INT32:
                rv = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                break;
            case INT16:
                rv = (short) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 2);
                _offsets[OFFSET_DATA] += 2;
                break;
            case UINT16:
                rv = new UInt16((int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 2));
                _offsets[OFFSET_DATA] += 2;
                break;
            case INT64:
                rv = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 8);
                _offsets[OFFSET_DATA] += 8;
                break;
            case UINT64:
                long top;
                long bottom;
                if (big) {
                    top = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                    _offsets[OFFSET_DATA] += 4;
                    bottom = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                } else {
                    bottom = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                    _offsets[OFFSET_DATA] += 4;
                    top = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                }
                rv = new UInt64(top, bottom);
                _offsets[OFFSET_DATA] += 4;
                break;
            case DOUBLE:
                long l = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 8);
                _offsets[OFFSET_DATA] += 8;
                rv = Double.longBitsToDouble(l);
                break;
            case FLOAT:
                int rf = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                rv = Float.intBitsToFloat(rf);
                break;
            case BOOLEAN:
                rf = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                rv = (1 == rf) ? Boolean.TRUE : Boolean.FALSE;
                break;
            case ARRAY:
                rv = extractArray(_signatureBuf, _dataBuf, _offsets, _options, this::extractOne);
                break;
            case STRUCT1:
                rv = extractStruct(_signatureBuf, _dataBuf, _offsets, _options, this::extractOne);
                break;
            case DICT_ENTRY1:
                Object[] decontents = new Object[2];

                LoggingHelper.logIf(logger.isTraceEnabled(), () ->
                    logger.trace("Extracting Dict Entry ({}) from: {}",
                            Hexdump.toAscii(_signatureBuf, _offsets[OFFSET_SIG], _signatureBuf.length - _offsets[OFFSET_SIG]),
                            Hexdump.toHex(_dataBuf, _offsets[OFFSET_DATA], _dataBuf.length - _offsets[OFFSET_DATA], true))
                );

                _offsets[OFFSET_SIG]++;
                decontents[0] = extractOne(_signatureBuf, _dataBuf, _offsets, ExtractOptions.copyWithContainedFlag(_options, true));
                _offsets[OFFSET_SIG]++;
                decontents[1] = extractOne(_signatureBuf, _dataBuf, _offsets, ExtractOptions.copyWithContainedFlag(_options, true));
                _offsets[OFFSET_SIG]++;
                rv = decontents;
                break;
            case VARIANT:
                rv = extractVariant(_dataBuf, _offsets, _options, (sig, obj) -> {
                    logger.trace("Creating Variant with SIG: {} - Value: {}", sig, obj);
                    return new Variant<>(obj, sig);
                });
                break;
            case FILEDESCRIPTOR:
                rv = filedescriptors.get((int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4));
                _offsets[OFFSET_DATA] += 4;
                break;
            case STRING:
                int length = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                rv = new String(_dataBuf, _offsets[OFFSET_DATA], length, StandardCharsets.UTF_8);
                _offsets[OFFSET_DATA] += length + 1;
                break;
            case OBJECT_PATH:
                length = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                rv = new DBusPath(getSource(), new String(_dataBuf, _offsets[OFFSET_DATA], length));
                _offsets[OFFSET_DATA] += length + 1;
                break;
            case SIGNATURE:
                length = _dataBuf[_offsets[OFFSET_DATA]++] & 0xFF;
                rv = new String(_dataBuf, _offsets[OFFSET_DATA], length);
                _offsets[OFFSET_DATA] += length + 1;
                break;
            default:
                throw new UnknownTypeCodeException(_signatureBuf[_offsets[OFFSET_SIG]]);
        }

        if (logger.isTraceEnabled()) {
            if (rv instanceof Object[] oa) {
                logger.trace("Extracted: {} (now at {})", Arrays.deepToString(oa), _offsets[OFFSET_DATA]);
            } else {
                logger.trace("Extracted: {} (now at {})", rv, _offsets[OFFSET_DATA]);
            }
        }

        return rv;
    }

    /**
     * Extracts a byte from the data received on bus.
     *
     * @param _dataBuf buffer holding the byte
     * @param _offsets offset position in buffer (will be updated)
     *
     * @return Object
     */
    private Object extractByte(byte[] _dataBuf, int[] _offsets) {
        Object rv;
        rv = _dataBuf[_offsets[OFFSET_DATA]++];
        return rv;
    }

    /**
     * Extracts a struct from the data received on bus.
     *
     * @param _signatureBuf signature (as byte array) defining the struct content
     * @param _dataBuf buffer containing the struct
     * @param _offsets offset position in buffer (will be updated)
     * @param _options extract options
     * @param _extractMethod method to be called for every entry contained of the struct
     *
     * @return Object
     *
     * @throws DBusException when parsing fails
     */
    private Object extractStruct(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, ExtractOptions _options, ExtractMethod _extractMethod) throws DBusException {
        Object rv;
        List<Object> contents = new ArrayList<>();
        while (_signatureBuf[++_offsets[OFFSET_SIG]] != STRUCT2) {
            contents.add(_extractMethod.extractOne(_signatureBuf, _dataBuf, _offsets, ExtractOptions.copyWithContainedFlag(_options, true)));
        }
        rv = contents.toArray();
        return rv;
    }

    /**
     * Extracts an array from the data received on bus.
     *
     * @param _signatureBuf signature string (as byte array) of the content of the array
     * @param _dataBuf buffer containing the array to read
     * @param _offsets current offsets in the buffer (will be updated)
     * @param _options additional options
     * @param _extractMethod method to be called for every entry contained in the array
     *
     * @return Object
     *
     * @throws MarshallingException when Array is too large
     * @throws DBusException when parsing fails
     */
    private Object extractArray(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, ExtractOptions _options, ExtractMethod _extractMethod)
            throws MarshallingException, DBusException {
        Object rv;
        long size = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);

        logger.trace("Reading array of size: {}", size);
        _offsets[OFFSET_DATA] += 4;
        byte algn = (byte) getAlignment(_signatureBuf[++_offsets[OFFSET_SIG]]);
        _offsets[OFFSET_DATA] = align(_offsets[OFFSET_DATA], _signatureBuf[_offsets[OFFSET_SIG]]);
        int length = (int) (size / algn);
        if (length > AbstractConnection.MAX_ARRAY_LENGTH) {
            throw new MarshallingException("Arrays must not exceed " + AbstractConnection.MAX_ARRAY_LENGTH);
        }

        rv = optimizePrimitives(_signatureBuf, _dataBuf, _offsets, size, algn, length, _options, _extractMethod);

        if (_options.contained() && !(rv instanceof List) && !(rv instanceof Map)) {
            rv = ArrayFrob.listify(rv);
        }
        return rv;
    }

    /**
     * Extracts a {@link Variant} from the data received on bus.
     *
     * @param _dataBuf buffer containing the variant
     * @param _offsets current offsets in the buffer (will be updated)
     * @param _options extract options
     * @param _variantFactory method to create new {@link Variant} objects (or other object types)
     *
     * @return Object / Variant
     *
     * @throws DBusException when parsing fails
     */
    private Object extractVariant(byte[] _dataBuf, int[] _offsets, ExtractOptions _options, BiFunction<String, Object, Object> _variantFactory) throws DBusException {
        Object rv;
        int[] newofs = new int[] {
                0, _offsets[OFFSET_DATA]
        };
        String sig = (String) extract(SIGNATURE_STRING, _dataBuf, newofs, _options)[0];
        newofs[OFFSET_SIG] = 0;
        rv = _variantFactory.apply(sig, extract(sig, _dataBuf, newofs, _options)[0]);
        _offsets[OFFSET_DATA] = newofs[OFFSET_DATA];

        return rv;
    }

    /**
     * Will create primitive arrays when an array is read.
     * <br>
     * In case the array is not compatible with primitives (e.g. object types are used or array contains Struct/Maps etc)
     * an array of the appropriate type will be created.
     *
     * @param _signatureBuf signature string (as byte array) containing the type of array
     * @param _dataBuf buffer containing the array
     * @param _offsets current offset in buffer (will be updated)
     * @param _size size of a byte
     * @param _algn data offset padding width when reading primitives (except byte)
     * @param _length length of the array
     * @param _options extract options
     * @param _extractMethod method to be called for every entry contained in the array if not primitive array
     *
     * @return Object array
     *
     * @throws DBusException when parsing fails
     */
    private Object optimizePrimitives(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, long _size, byte _algn,
            int _length, ExtractOptions _options, ExtractMethod _extractMethod)
            throws DBusException {
        Object rv = null;

        int offsetPos = _offsets[OFFSET_SIG] - 1; // need to extract one because extractArray will already update offset position
        boolean optimize = _options.arrayConvert() != null
            && _options.arrayConvert().size() > offsetPos
            && _options.arrayConvert().get(offsetPos) == ConstructorArgType.PRIMITIVE_ARRAY;

        if (optimize) {
            switch (_signatureBuf[_offsets[OFFSET_SIG]]) {
                case BYTE:
                    rv = new byte[_length];
                    System.arraycopy(_dataBuf, _offsets[OFFSET_DATA], rv, 0, _length);
                    _offsets[OFFSET_DATA] += _size;
                    break;
                case INT16:
                    rv = new short[_length];
                    for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                        ((short[]) rv)[j] = (short) demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
                    }
                    break;
                case INT32:
                    rv = new int[_length];
                    for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                        ((int[]) rv)[j] = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
                    }
                    break;
                case INT64:
                    rv = new long[_length];
                    for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                        ((long[]) rv)[j] = demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
                    }
                    break;
                case BOOLEAN:
                    rv = new boolean[_length];
                    for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                        ((boolean[]) rv)[j] = 1 == demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
                    }
                    break;
                case FLOAT:
                    rv = new float[_length];
                    for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                        ((float[]) rv)[j] = Float.intBitsToFloat((int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn));
                    }
                    break;
                case DOUBLE:
                    rv = new double[_length];
                    for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                        ((double[]) rv)[j] = Double.longBitsToDouble(demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn));
                    }
                    break;
                default:
                    break;
            }
        }
        if (_signatureBuf[_offsets[OFFSET_SIG]] == DICT_ENTRY1) {
            int ofssave = prepareCollection(_signatureBuf, _offsets, _size);
            long end = _offsets[OFFSET_DATA] + _size;

            Map<Object, Object> map = new LinkedHashMap<>();
            while (_offsets[OFFSET_DATA] < end) {
                _offsets[OFFSET_SIG] = ofssave;
                Object[] data = (Object[]) _extractMethod.extractOne(_signatureBuf, _dataBuf, _offsets, ExtractOptions.copyWithContainedFlag(_options, true));

                map.put(data[0], data[1]);
            }

            rv = map;
        }

        if (rv == null) {
            int ofssave = prepareCollection(_signatureBuf, _offsets, _size);
            long end = _offsets[OFFSET_DATA] + _size;
            List<Object> contents = new ArrayList<>();
            while (_offsets[OFFSET_DATA] < end) {
                _offsets[OFFSET_SIG] = ofssave;
                contents.add(_extractMethod.extractOne(_signatureBuf, _dataBuf, _offsets, ExtractOptions.copyWithContainedFlag(_options, true)));
            }
            rv = contents;
        }
        return rv;
    }

    private int prepareCollection(byte[] _signatureBuf, int[] _offsets, long _size) throws DBusException {
        if (0 == _size) {
            // advance the type parser even on 0-size arrays.
            List<Type> temp = new ArrayList<>();
            byte[] temp2 = new byte[_signatureBuf.length - _offsets[OFFSET_SIG]];
            System.arraycopy(_signatureBuf, _offsets[OFFSET_SIG], temp2, 0, temp2.length);
            String temp3 = new String(temp2);
            // ofs[OFFSET_SIG] gets incremented anyway. Leave one character on the stack
            int temp4 = Marshalling.getJavaType(temp3, temp, 1) - 1;
            _offsets[OFFSET_SIG] += temp4;
            logger.trace("Aligned type: {} {} {}", temp3, temp4, _offsets[OFFSET_SIG]);
        }
        return _offsets[OFFSET_SIG];
    }

    /**
     * Demarshall values from a buffer.
     *
     * @param _signature The D-Bus signature(s) of the value(s).
     * @param _dataBuf The buffer to demarshall from.
     * @param _offsets The offset into the data buffer to start.
     * @param _options additional options
     * @return The demarshalled value(s).
     *
     * @throws DBusException on error
     */
    protected Object[] extract(String _signature, byte[] _dataBuf, int _offsets, ExtractOptions _options) throws DBusException {
        return extract(_signature, _dataBuf, new int[] {
                0, _offsets
        }, _options);
    }

    /**
     * Demarshall values from a buffer.
     *
     * @param _signature The D-Bus signature(s) of the value(s).
     * @param _dataBuf The buffer to demarshall from.
     * @param _offsets An array of two ints, which holds the position of the current signature offset and the current
     *            offset of the data buffer. These values will be updated to the start of the next value after
     *            demarshalling.
     * @param _options additional options
     *
     * @return The demarshalled value(s).
     *
     * @throws DBusException on error
     */
    protected Object[] extract(String _signature, byte[] _dataBuf, int[] _offsets, ExtractOptions _options) throws DBusException {
        return extract(_signature, _dataBuf, _offsets, _options, this::extractOne);
    }

    Object[] extract(String _signature, byte[] _dataBuf, int[] _offsets, ExtractOptions _options, ExtractMethod _method) throws DBusException {
        logger.trace("extract({},#{}, {{},{}}", _signature, _dataBuf.length, _offsets[OFFSET_SIG],
                _offsets[OFFSET_DATA]);
        List<Object> rv = new ArrayList<>();
        byte[] sigb = _signature.getBytes();
        for (int[] i = _offsets; i[OFFSET_SIG] < sigb.length; i[OFFSET_SIG]++) {
            rv.add(_method.extractOne(sigb, _dataBuf, i, ExtractOptions.copyWithContainedFlag(_options, false)));
        }

        return rv.toArray();
    }

    /**
     * Returns the Bus ID that sent the message.
     *
     * @return string
     */
    public String getSource() {
        return (String) getHeader(HeaderField.SENDER);
    }

    /**
     * Returns the destination of the message.
     *
     * @return string
     */
    public String getDestination() {
        return (String) getHeader(HeaderField.DESTINATION);
    }

    /**
     * Returns the interface of the message.
     *
     * @return string
     */
    public String getInterface() {
        return (String) getHeader(HeaderField.INTERFACE);
    }

    /**
     * Returns the object path of the message.
     *
     * @return string
     */
    public String getPath() {
        Object o = getHeader(HeaderField.PATH);
        if (null == o) {
            return null;
        }
        return o.toString();
    }

    /**
     * Returns the member name or error name this message represents.
     *
     * @return string
     */
    public String getName() {
        if (this instanceof Error) {
            return (String) getHeader(HeaderField.ERROR_NAME);
        } else {
            return (String) getHeader(HeaderField.MEMBER);
        }
    }

    /**
     * Returns the dbus signature of the parameters.
     *
     * @return string
     */
    public String getSig() {
        return (String) getHeader(HeaderField.SIGNATURE);
    }

    /**
     * Returns the message flags.
     *
     * @return int
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns the message serial ID (unique for this connection)
     *
     * @return the message serial.
     */
    public synchronized long getSerial() {
        return serial;
    }

    /**
     * If this is a reply to a message, this returns its serial.
     *
     * @return The reply serial, or 0 if it is not a reply.
     */
    public long getReplySerial() {
        Number l = (Number) getHeader(HeaderField.REPLY_SERIAL);
        if (null == l) {
            return 0;
        }
        return l.longValue();
    }

    /**
     * Parses and returns the parameters to this message as an Object array.
     *
     * @return object array
     * @throws DBusException on failure
     */
    public Object[] getParameters() throws DBusException {
        return getParameters(null);
    }

    /**
     * Parses and returns the parameters to this message as an Object array.
     *
     * This method takes a list of Type[] where each entry of the list represents a constructor call.
     * The constructor arguments are used to determine if a collection of array of
     * primitive type should be used when calling the constructor.
     *
     * @param _constructorArgs list of desired constructor arguments
     * @return object array
     * @throws DBusException on failure
     */
    public Object[] getParameters(List<Type[]> _constructorArgs) throws DBusException {
        if (null == args && body != null) {
            args = extractArgs(_constructorArgs);
        }
        return args;
    }

    /**
     * Creates a object array containing all objects which should be used to call a constructor.
     *
     * @param _constructorArgs list of desired constructor arguments
     * @return object array
     * @throws DBusException on failure
     */
    private Object[] extractArgs(List<Type[]> _constructorArgs) throws DBusException {
        String sig = getSig();

        ExtractOptions options = DEFAULT_OPTIONS;
        if (_constructorArgs != null && !_constructorArgs.isEmpty()) {
            List<Type> dataType = new ArrayList<>();
            Marshalling.getJavaType(getSig(), dataType, -1);
            options = new ExtractOptions(DEFAULT_OPTIONS.contained(), usesPrimitives(_constructorArgs, dataType));
        }

        if (sig != null && body != null && body.length != 0) {
            return extract(sig, body, 0, options);
        }
        return new Object[0];
    }

    /**
     * Compares a list of Type[] with a list of desired types.
     * This is used to decide if an array of primitive types,
     * an array of object types or a Collection/List of a object type is used in the constructor.
     *
     * @param _constructorArgs list of constructor types to check
     * @param _dataType list of desired types
     *
     * @return List of ConstructorArgType
     */
    static List<ConstructorArgType> usesPrimitives(List<Type[]> _constructorArgs, List<Type> _dataType) {
        Logger logger = LoggerFactory.getLogger(Message.class);
        OUTER: for (Type[] ptype : _constructorArgs) {
            if (ptype.length == _dataType.size()) {
                List<ConstructorArgType> argTypes = new ArrayList<>();

                for (int i = 0; i < ptype.length; i++) {
                    logger.trace(">>>>>> Comparing {} with {}", ptype[i], _dataType.get(i));
                    // this is a list type and an array should be used
                    if (ptype[i] instanceof Class<?> constructorClz && constructorClz.isArray()
                            && _dataType.get(i) instanceof ParameterizedType pt
                            && pt.getRawType() == List.class
                            && pt.getActualTypeArguments().length > 0
                            && pt.getActualTypeArguments()[0] instanceof Class<?> sigExpectedClz) {

                        logger.trace("Found List type when array was required, trying to find proper array type");
                        if (PrimitiveUtils.isCompatiblePrimitiveOrWrapper(constructorClz.getComponentType(), sigExpectedClz)) {
                            ConstructorArgType type = constructorClz.getComponentType().isPrimitive() ? ConstructorArgType.PRIMITIVE_ARRAY : ConstructorArgType.ARRAY;
                            logger.trace("Selecting {} for parameter {} <=> {}", type, constructorClz.getComponentType(), sigExpectedClz);
                            argTypes.add(type);
                        } else {
                            logger.trace("List uses a different type than required. Found {}, required {}", constructorClz.getComponentType(), sigExpectedClz);
                            continue OUTER;
                        }
                    } else if (ptype[i] instanceof Class<?> clz
                        && _dataType.get(i) instanceof ParameterizedType pt
                        && clz.isAssignableFrom((Class<?>) pt.getRawType())
                        && Collection.class.isAssignableFrom(clz)) {

                        logger.trace("Found compatible collection type: {} <=> {}", clz, pt.getRawType());
                        // the constructor wants some sort of collection
                        argTypes.add(ConstructorArgType.COLLECTION);

                    } else if (ptype[i] instanceof Class<?> constructorClz
                        && _dataType.get(i) instanceof Class<?> sigExpectedClz
                        && !sigExpectedClz.isAssignableFrom(constructorClz)
                        && !PrimitiveUtils.isCompatiblePrimitiveOrWrapper(constructorClz, sigExpectedClz)
                        ) {
                        logger.trace("Constructor data type mismatch, must be wrong constructor ({} != {})", constructorClz, sigExpectedClz);
                        // constructor class type does not match, must be wrong constructor, try next
                        continue OUTER;
                    } else {
                        // not a list/array and type matches, no conversion needed
                        logger.trace("Type {} is not an array type, skipping", ptype[i]);
                        argTypes.add(ConstructorArgType.NOT_ARRAY_TYPE);
                    }
                }
                return argTypes;
            }
        }
        logger.trace("No matching constructor arguments found");
        return List.of();
    }

    public void setArgs(Object[] _args) {
        this.args = _args;
    }

    /**
     * Warning, do not use this method unless you really know what you are doing.
     *
     * @param _source string
     * @throws DBusException on error
     */
    public void setSource(String _source) throws DBusException {
        if (null != body) {
            logger.trace("Setting source");

            LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("WireData before: {}", dumpWireData()));

            wiredata = new byte[BUFFERINCREMENT][];
            bufferuse = 0;
            bytecounter = 0;
            preallocate(12);
            append("yyyyuu", big ? Endian.BIG : Endian.LITTLE, type, flags, protover, bodylen, getSerial());
            headers[HeaderField.SENDER] = _source;

            LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("WireData first append: {}", dumpWireData()));

            List<Object[]> newHeader = new ArrayList<>(headers.length);

            for (int hIdx = 0; hIdx < headers.length; hIdx++) {
                Object object = headers[hIdx];

                if (object == null) {
                    continue;
                }
                if (hIdx == HeaderField.SIGNATURE) {
                    newHeader.add(createHeaderArgs(HeaderField.SIGNATURE, SIGNATURE_STRING, object));
                } else {
                    newHeader.add(new Object[] {hIdx, object});
                }
            }

            append("a(yv)", newHeader);

            LoggingHelper.logIf(logger.isTraceEnabled(), () -> {
                logger.trace("New header: {}", LoggingHelper.arraysVeryDeepString(newHeader.toArray()));
                logger.trace("WireData after: {}", dumpWireData());
            });

            pad((byte) 8);
            appendBytes(body);
        }
    }

    /**
     * Dumps the current content of {@link #wiredata} to String.
     *
     * @return String, maybe empty
     * @since v4.2.2 - 2023-01-20
     */
    String dumpWireData() {
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        for (int i = 0; i < wiredata.length; i++) {
            byte[] arr = wiredata[i];
            if (arr != null) {
                String prefix = "Wiredata[" + i + "]";
                String format = Hexdump.format(arr, 80);
                String[] split = format.split("\n");
                sb.append(prefix).append(": ").append(split[0]).append(System.lineSeparator());
                if (split.length > 1) {
                    sb.append(Arrays.stream(split)
                            .skip(1)
                            .map(s -> String.format("%s: %80s", prefix, s))
                            .collect(Collectors.joining(System.lineSeparator())));
                    sb.append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Type of this message.
     * @return byte
     */
    public byte getType() {
        return type;
    }

    public byte getEndianess() {
        if (endianWasSet) {
            return big ? Endian.BIG : Endian.LITTLE;
        }
        return 0;
    }

    /**
     * Creates a message header.
     * Will automatically add the values to the current instances header map.
     *
     * @param _header header type (one of {@link HeaderField})
     * @param _argType argument type (one of {@link ArgumentType})
     * @param _value value
     *
     * @return Object array
     */
    protected Object[] createHeaderArgs(byte _header, String _argType, Object _value) {
        getHeader()[_header] =  _value;
        return new Object[] {
                _header, new Object[] {
                        _argType, _value
                }
        };
    }

    /**
     * Adds message padding and marshalling.
     *
     * @param _hargs
     * @param _serial
     * @param _sig
     * @param _args
     * @throws DBusException
     */
    protected void padAndMarshall(List<Object> _hargs, long _serial, String _sig, Object... _args) throws DBusException {
        byte[] blen = new byte[4];
        appendBytes(blen);
        append("ua(yv)", _serial, _hargs.toArray());
        pad((byte) 8);

        long c = getByteCounter();
        if (null != _sig) {
            append(_sig, _args);
        }
        logger.trace("Appended body, type: {} start: {} end: {} size: {}", _sig, c, getByteCounter(), getByteCounter() - c);
        marshallint(getByteCounter() - c, blen, 0, 4);
        LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("marshalled size ({}): {}", blen, Hexdump.format(blen)));
    }

    /**
     * Demarshalls an integer of a given width from a buffer.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _endian The endianness to use in demarshalling.
     * @param _width The byte-width of the int.
     *
     * @return long
     */
    public static long demarshallint(byte[] _buf, int _ofs, byte _endian, int _width) {
        return _endian == Endian.BIG ? demarshallintBig(_buf, _ofs, _width) : demarshallintLittle(_buf, _ofs, _width);
    }

    /**
     * Demarshalls an integer of a given width from a buffer using big-endian format.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _width The byte-width of the int.
     * @return long
     */
    public static long demarshallintBig(byte[] _buf, int _ofs, int _width) {
        long l = 0;
        for (int i = 0; i < _width; i++) {
            l <<= 8;
            l |= _buf[_ofs + i] & 0xFF;
        }
        return l;
    }

    /**
     * Demarshalls an integer of a given width from a buffer using little-endian format.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _width The byte-width of the int.
     *
     * @return long
     */
    public static long demarshallintLittle(byte[] _buf, int _ofs, int _width) {
        long l = 0;
        for (int i = _width - 1; i >= 0; i--) {
            l <<= 8;
            l |= _buf[_ofs + i] & 0xFF;
        }
        return l;
    }

    /**
     * Marshalls an integer of a given width into a buffer using big-endian format.
     *
     * @param _l The integer to marshall.
     * @param _buf The buffer to marshall to.
     * @param _ofs The offset to marshall to.
     * @param _width The byte-width of the int.
     */
    public static void marshallintBig(long _l, byte[] _buf, int _ofs, int _width) {
        long l = _l;
        for (int i = _width - 1; i >= 0; i--) {
            _buf[i + _ofs] = (byte) (l & 0xFF);
            l >>= 8;
        }
    }

    /**
     * Marshalls an integer of a given width into a buffer using little-endian format.
     *
     * @param _l The integer to marshall.
     * @param _buf The buffer to demarshall to.
     * @param _ofs The offset to demarshall to.
     * @param _width The byte-width of the int.
     */
    public static void marshallintLittle(long _l, byte[] _buf, int _ofs, int _width) {
        long l = _l;
        for (int i = 0; i < _width; i++) {
            _buf[i + _ofs] = (byte) (l & 0xFF);
            l >>= 8;
        }
    }

    /**
     * Return the alignment for a given type.
     *
     * @param _type type
     * @return int
     */
    public static int getAlignment(byte _type) {
        return switch (_type) {
            case 2, INT16, UINT16 -> 2;
            case 4, BOOLEAN, FLOAT, INT32, UINT32, FILEDESCRIPTOR, STRING, OBJECT_PATH, ARRAY -> 4;
            case 8, INT64, UINT64, DOUBLE, STRUCT, DICT_ENTRY, STRUCT1, DICT_ENTRY1, STRUCT2, DICT_ENTRY2 -> 8;
            case 1, BYTE, SIGNATURE, VARIANT -> 1;
            default -> 1;
        };
    }

    /**
     * Returns the name of the given header field.
     *
     * @param _field field
     * @return string
     */
    public static String getHeaderFieldName(byte _field) {
        return switch (_field) {
            case HeaderField.PATH -> "Path";
            case HeaderField.INTERFACE -> "Interface";
            case HeaderField.MEMBER -> "Member";
            case HeaderField.ERROR_NAME -> "Error Name";
            case HeaderField.REPLY_SERIAL -> "Reply Serial";
            case HeaderField.DESTINATION -> "Destination";
            case HeaderField.SENDER -> "Sender";
            case HeaderField.SIGNATURE -> "Signature";
            case HeaderField.UNIX_FDS -> "Unix FD";
            default -> "Invalid";
        };
    }

    /**
     * Creates a clone of this message and setting serial to next available.
     * @return Message
     */
    Message cloneWithNewSerial() {
        Message message = new Message();

        byte[][] copyWireData = new byte[wiredata.length][];

        for (int i = 0; i < wiredata.length; i++) {
            if (wiredata[i] != null) {
                copyWireData[i] = Arrays.copyOf(wiredata[i], wiredata[i].length);
            }
        }

        message.body = Arrays.copyOf(body, body.length);
        message.endianWasSet = true;
        message.big = big;
        message.flags = flags;
        message.protover = protover;
        message.type = type;
        message.bufferuse = bufferuse;
        message.bodylen = bodylen;
        message.bytecounter = bytecounter;
        message.filedescriptors.addAll(filedescriptors);

        message.setWireData(copyWireData);
        message.setHeader(Arrays.copyOf(headers, headers.length));
        message.setSerial(GLOBAL_SERIAL.incrementAndGet());

        return message;
    }

    /**
     * Interface defining a method to extract a specific data type.
     * For internal usage only.
     *
     * @since 4.2.0 - 2022-08-19
     */
    @FunctionalInterface
    interface ExtractMethod {
        Object extractOne(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, ExtractOptions _options)
                throws DBusException;
    }

    /**
     * Additional options to optimize value extraction.
     * @param contained boolean to indicate if nested lists should be resolved (false usually)
     * @param arrayConvert use Collection, array or array of primitives
     * @since 5.1.0 - 2024-05-18
     */
    record ExtractOptions(
        boolean contained,
        List<ConstructorArgType> arrayConvert
        ) {

        static ExtractOptions copyWithContainedFlag(ExtractOptions _toCopy, boolean _containedFlag) {
            return new ExtractOptions(_containedFlag, _toCopy.arrayConvert());
        }
    }

    enum ConstructorArgType {
        PRIMITIVE_ARRAY, ARRAY, COLLECTION, NOT_ARRAY_TYPE;
    }
}
