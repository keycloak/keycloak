package org.freedesktop.dbus.spi.message;

import java.io.Closeable;
import java.io.IOException;

import org.freedesktop.dbus.messages.Message;

/**
 * Interface that lets you write a message to the currently used transport.
 */
public interface IMessageWriter extends Closeable {

    /**
     * Write a message out to the bus.
     *
     * @param _msg The message to write
     * @throws IOException If an IO error occurs.
     */
    void writeMessage(Message _msg) throws IOException;

    boolean isClosed();
}
