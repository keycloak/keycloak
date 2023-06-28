package org.freedesktop.dbus.utils;

import java.io.PrintStream;

public final class Hexdump {
    public static final char[] HEX_CHARS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private Hexdump() {

    }

    public static String toHex(byte[] _buf) {
        return toHex(_buf, true);
    }

    public static String toHex(byte[] _buf, boolean _spaces) {
        return toHex(_buf, 0, _buf.length, _spaces);
    }

    public static String toHex(byte[] _buf, int _ofs, int _len, boolean _spaces) {
        StringBuilder sb = new StringBuilder();
        int j = _ofs + _len;
        for (int i = _ofs; i < j; i++) {
            if (i < _buf.length) {
                sb.append(HEX_CHARS[(_buf[i] & 0xF0) >> 4]);
                sb.append(HEX_CHARS[_buf[i] & 0x0F]);
                if (_spaces) {
                    sb.append(' ');
                }
            } else {
                if (_spaces) {
                    sb.append(' ');
                    sb.append(' ');
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    public static String toAscii(byte[] _buf) {
        return toAscii(_buf, 0, _buf.length);
    }

    public static String toAscii(byte[] _buf, int _ofs, int _len) {
        StringBuilder sb = new StringBuilder();
        int j = _ofs + _len;
        for (int i = _ofs; i < j; i++) {
            if (i < _buf.length) {
                if (20 <= _buf[i] && 126 >= _buf[i]) {
                    sb.append((char) _buf[i]);
                } else {
                    sb.append('.');
                }
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    public static String format(byte[] _buf) {
        return format(_buf, 80);
    }

    public static String format(byte[] _buf, int _width) {
        int bs = (_width - 8) / 4;
        int i = 0;
        StringBuilder sb = new StringBuilder();
        do {
            for (int j = 0; j < 6; j++) {
                sb.append(HEX_CHARS[(i << (j * 4) & 0xF00000) >> 20]);
            }
            sb.append('\t');
            sb.append(toHex(_buf, i, bs, true));
            sb.append(' ');
            sb.append(toAscii(_buf, i, bs));
            sb.append('\n');
            i += bs;
        } while (i < _buf.length);
        sb.deleteCharAt(sb.length() - 1); // remove the last \n
        return sb.toString();
    }

    public static void print(byte[] _buf) {
        print(_buf, System.err);
    }

    public static void print(byte[] _buf, int _width) {
        print(_buf, _width, System.err);
    }

    public static void print(byte[] _buf, int _width, PrintStream _out) {
        _out.print(format(_buf, _width));
    }

    public static void print(byte[] _buf, PrintStream _out) {
        _out.print(format(_buf));
    }

    /**
     * Returns a string which can be written to a Java source file as part
     * of a static initializer for a byte array.
     * Returns data in the format 0xAB, 0xCD, ....
     * use like:
     * javafile.print("byte[] data = {")
     * javafile.print(Hexdump.toByteArray(data));
     * javafile.println("};");     * @param buf
     * @param _buf buffer
     * @return string
     */
    public static String toByteArray(byte[] _buf) {
        return toByteArray(_buf, 0, _buf.length);
    }

    /**
     * Returns a string which can be written to a Java source file as part
     * of a static initializer for a byte array.
     * Returns data in the format 0xAB, 0xCD, ....
     * use like:
     * javafile.print("byte[] data = {")
     * javafile.print(Hexdump.toByteArray(data));
     * javafile.println("};");
     *
     * @param _buf buffer
     * @param _ofs offset
     * @param _len length
     * @return string
     */
    public static String toByteArray(byte[] _buf, int _ofs, int _len) {
        StringBuilder sb = new StringBuilder();
        for (int i = _ofs; i < _len && i < _buf.length; i++) {
            sb.append('0');
            sb.append('x');
            sb.append(HEX_CHARS[(_buf[i] & 0xF0) >> 4]);
            sb.append(HEX_CHARS[_buf[i] & 0x0F]);
            if ((i + 1) < _len && (i + 1) < _buf.length) {
                sb.append(',');
            }
        }
        return sb.toString();
    }
}
