package org.freedesktop.dbus.spi.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Objects;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class which can be used to implement a custom message writer.
 *
 * @since 4.3.1 - 2023-08-07
 */
public abstract class AbstractOutputStreamMessageWriter implements IMessageWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SocketChannel outputChannel;

    private final ISocketProvider socketProviderImpl;

    protected AbstractOutputStreamMessageWriter(final SocketChannel _out, ISocketProvider _socketProviderImpl) {
        outputChannel = Objects.requireNonNull(_out, "SocketChannel required");
        socketProviderImpl = Objects.requireNonNull(_socketProviderImpl, "ISocketProvider implementation required");
    }

    @Override
    public final void writeMessage(Message _msg) throws IOException {
        logger.debug("<= {}", _msg);
        if (null == _msg) {
            return;
        }
        if (null == _msg.getWireData()) {
            logger.warn("Message {} wire-data was null!", _msg);
            return;
        }

        if (socketProviderImpl.isFileDescriptorPassingSupported()) {
            writeFileDescriptors(outputChannel, _msg.getFiledescriptors());
        }

        for (byte[] buf : _msg.getWireData()) {
            if (logger.isTraceEnabled()) {
                logger.trace("{}", null == buf ? "(buffer was null)" : Hexdump.format(buf));
            }
            if (null == buf) {
                break;
            }

            outputChannel.write(ByteBuffer.wrap(buf));
        }

        logger.trace("Message sent: {}", _msg);
    }

    /**
     * Called to write any file descriptors to the given channel.<br>
     * Should do nothing if there is no file descriptor to write, or method is not supported.
     *
     * @param _outputChannel channel to write to
     * @param _filedescriptors file descriptors attached to message
     *
     * @throws IOException when writing the descriptors fail
     */
    protected abstract void writeFileDescriptors(SocketChannel _outputChannel, List<FileDescriptor> _filedescriptors) throws IOException;

    protected Logger getLogger() {
        return logger;
    }

    protected ISocketProvider getSocketProviderImpl() {
        return socketProviderImpl;
    }

    @Override
    public void close() throws IOException {
        logger.debug("Closing Message Writer");
        if (outputChannel.isOpen()) {
            outputChannel.close();
            logger.debug("Message Writer closed");
        }
    }

    @Override
    public boolean isClosed() {
        return !outputChannel.isOpen();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [outputChannel=" + outputChannel + ", socketProviderImpl=" + socketProviderImpl + "]";
    }

}
