package org.freedesktop.dbus.spi.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.freedesktop.dbus.connections.transports.AbstractTransport;

public interface ISocketProvider {
    /**
     * Method to create a {@link IMessageReader} implementation.
     *
     * @param _socket socket to use for reading
     * @return MessageReader
     * @throws IOException if reader could not be created
     */
    IMessageReader createReader(SocketChannel _socket) throws IOException;

    /**
     * Method to create a {@link IMessageWriter} implementation.
     *
     * @param _socket socket to write to
     * @return MessageWriter
     * @throws IOException if write could not be created
     */
    IMessageWriter createWriter(SocketChannel _socket) throws IOException;

    /**
     * Called to indicate if the current {@link AbstractTransport} implementation
     * supports file descriptor passing.
     *
     * @param _support true if file descriptor passing is supported, false otherwise
     */
    void setFileDescriptorSupport(boolean _support);

    /**
     * Indicate if reader/writer supports file descriptor passing.
     * This is to show if the provider is able to handle file descriptors.
     *
     * @return true if file descriptors are supported by this provider, false otherwise
     */
    boolean isFileDescriptorPassingSupported();
}
