package org.freedesktop.dbus.connections.transports;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.freedesktop.dbus.spi.message.IMessageReader;
import org.freedesktop.dbus.spi.message.IMessageWriter;

/**
 * Represents one transport connection of any type.<br>
 * <p>
 * A transport connection is bound to a SocketChannel which might be
 * a connection to a DBusServer when used as client or a connection
 * from a client when running as server.
 * </p>
 *
 * @author hypfvieh
 * @since v4.2.2 - 2023-02-02
 */
public class TransportConnection implements Closeable {
    private static final AtomicLong TRANSPORT_ID_GENERATOR = new AtomicLong(0);

    private final long id = TRANSPORT_ID_GENERATOR.incrementAndGet();
    private final SocketChannel channel;
    private final IMessageWriter writer;
    private final IMessageReader reader;

    public TransportConnection(SocketChannel _channel, IMessageWriter _writer, IMessageReader _reader) {
        channel = _channel;
        writer = _writer;
        reader = _reader;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public IMessageWriter getWriter() {
        return writer;
    }

    public IMessageReader getReader() {
        return reader;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + id + ", channel=" + channel + ", writer=" + writer + ", reader="
                + reader + "]";
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }

        if (writer != null) {
            writer.close();
        }

        if (channel != null) {
            channel.close();
        }
    }

}
