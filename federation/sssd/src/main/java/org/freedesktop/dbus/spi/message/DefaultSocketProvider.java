package org.freedesktop.dbus.spi.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Default internally used socket provider implementation.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-09
 */
final class DefaultSocketProvider implements ISocketProvider {

    static final ISocketProvider INSTANCE = new DefaultSocketProvider();

    private DefaultSocketProvider() {

    }

    @Override
    public IMessageReader createReader(SocketChannel _socket) throws IOException {
        return new InputStreamMessageReader(_socket);
    }

    @Override
    public IMessageWriter createWriter(SocketChannel _socket) throws IOException {
        return new OutputStreamMessageWriter(_socket);
    }

    @Override
    public void setFileDescriptorSupport(boolean _support) {
        // not supported
    }

    @Override
    public boolean isFileDescriptorPassingSupported() {
        return false;
    }

}
