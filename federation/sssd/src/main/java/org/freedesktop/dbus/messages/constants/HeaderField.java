package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each valid header field type.
 * @since 5.0.0 - 2023-10-23
 */
public final class HeaderField {
    public static final int MAX_FIELDS    = 10;

    public static final byte PATH         = 1;
    public static final byte INTERFACE    = 2;
    public static final byte MEMBER       = 3;
    public static final byte ERROR_NAME   = 4;
    public static final byte REPLY_SERIAL = 5;
    public static final byte DESTINATION  = 6;
    public static final byte SENDER       = 7;
    public static final byte SIGNATURE    = 8;
    public static final byte UNIX_FDS     = 9;

    private HeaderField() {

    }
}
