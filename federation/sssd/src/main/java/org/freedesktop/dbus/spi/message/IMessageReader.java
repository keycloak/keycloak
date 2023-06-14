package org.freedesktop.dbus.spi.message;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents a way to read messages from the bus.
 */
public interface IMessageReader extends Closeable {

    boolean isClosed();

    Message readMessage() throws IOException, DBusException;
}
