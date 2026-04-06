package org.freedesktop.dbus.spi.message;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Optional;

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

    /**
     * Attempts to extract raw FileDescriptor value from {@link FileDescriptor} instance.
     * Note that not any {@link FileDescriptor} can be represented as int, for example Windows uses HANDLE as descriptor,
     * which excess range of int values, thus cannot be safely cast to int.
     *
     * @param _fd FileDescriptor to extract value from
     * @return int representation, packed to {@link Optional} if operation succeeded, or {@link Optional#empty()} otherwise
     * @see #createFileDescriptor(int)
     * @since 5.0.0 - 2023-10-07
     */
    default Optional<Integer> getFileDescriptorValue(FileDescriptor _fd) {
        return Optional.empty();
    }

    /**
     * Attempts to create native {@link FileDescriptor} from raw int value.
     *
     * @param _fd FileDescriptor to extract value from
     * @return {@link FileDescriptor}, instantiated with provided value, packed to {@link Optional} if operation succeeded, or {@link Optional#empty()} otherwise
     * @see #getFileDescriptorValue(FileDescriptor)
     * @since 5.0.0 - 2023-10-07
     */
    default Optional<FileDescriptor> createFileDescriptor(int _fd) {
        return Optional.empty();
    }
}
