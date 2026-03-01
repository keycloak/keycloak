package org.freedesktop.dbus.spi.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputStreamMessageWriter implements IMessageWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SocketChannel outputChannel;

    public OutputStreamMessageWriter(SocketChannel _out) {
        this.outputChannel = _out;
    }

    @Override
    public void writeMessage(Message _msg) throws IOException {
        logger.debug("<= {}", _msg);
        if (null == _msg) {
            return;
        }
        if (null == _msg.getWireData()) {
            logger.warn("Message {} wire-data was null!", _msg);
            return;
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

    @Override
    public void close() throws IOException {
        logger.debug("Closing Message Writer");
        if (outputChannel != null && outputChannel.isOpen()) {
            outputChannel.close();
        }
        outputChannel = null;
    }

    @Override
    public boolean isClosed() {
        return outputChannel != null && !outputChannel.isOpen();
    }
}
