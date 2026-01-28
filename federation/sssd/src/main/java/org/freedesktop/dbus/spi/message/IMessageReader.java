package org.freedesktop.dbus.spi.message;

import java.io.Closeable;
import java.io.IOException;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;

/**
 * Represents a way to read messages from the bus.
 */
public interface IMessageReader extends Closeable {

    boolean isClosed();

    Message readMessage() throws IOException, DBusException;
}
