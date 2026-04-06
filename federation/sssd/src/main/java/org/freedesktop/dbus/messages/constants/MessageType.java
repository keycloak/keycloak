package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each message type.
 * @since 5.0.0 - 2023-10-23
 * @deprecated use MessageTypes instead
 */
@Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
public final class MessageType {
    public static final byte METHOD_CALL   = MessageTypes.METHOD_CALL.getId();
    public static final byte METHOD_RETURN = MessageTypes.METHOD_REPLY.getId();
    public static final byte ERROR         = MessageTypes.ERROR.getId();
    public static final byte SIGNAL        = MessageTypes.SIGNAL.getId();

    private MessageType() {

    }
}
