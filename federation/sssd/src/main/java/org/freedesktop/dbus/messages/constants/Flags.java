package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants representing the flags which can be set on a message.
 * @since 5.0.0 - 2023-10-23
 */
public final class Flags {
    public static final byte NO_REPLY_EXPECTED = 0x01;
    public static final byte NO_AUTO_START     = 0x02;
    public static final byte ASYNC             = 0x40;

    private Flags() {

    }
}
