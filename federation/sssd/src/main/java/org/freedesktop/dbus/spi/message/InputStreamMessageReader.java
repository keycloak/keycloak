package org.freedesktop.dbus.spi.message;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageProtocolVersionException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputStreamMessageReader implements IMessageReader {
    private final Logger        logger = LoggerFactory.getLogger(getClass());

    private final int[]         len;
    private final byte[]        buf;
    private final byte[]        tbuf;
    private final SocketChannel inputChannel;

    private byte[]              header;
    private byte[]              body;

    public InputStreamMessageReader(final SocketChannel _in) {
        inputChannel = Objects.requireNonNull(_in, "SocketChannel required");
        len = new int[4];
        tbuf = new byte[4];
        buf = new byte[12];
        len[1] = 0;
        len[0] = 0;
    }

    @Override
    public Message readMessage() throws IOException, DBusException {
        /* Read the 12 byte fixed header, retrying as necessary */
        if (len[0] < 12) {
            try {
                final ByteBuffer wrapBuf = ByteBuffer.wrap(buf, len[0], 12 - len[0]);
                final int rv = inputChannel.read(wrapBuf);

                if (rv < 0) {
                    throw new EOFException("(1) Underlying transport returned " + rv);
                }

                len[0] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[0] == 0) {
            return null;
        }

        if (len[0] < 12) {
            logger.trace("Only got {} of 12 bytes of header", len[0]);
            return null;
        }

        /* Ensure protocol version. */
        final byte protoVer = buf[3];

        if (protoVer > Message.PROTOCOL) {
            throw new MessageProtocolVersionException(String.format("Protocol version %s is unsupported", protoVer));
        }

        if (len[1] < 4) {
            try {
                final int rv = inputChannel.read(ByteBuffer.wrap(tbuf, len[1], 4 - len[1]));

                if (rv < 0) {
                    throw new EOFException("(2) Underlying transport returned " + rv);
                }

                len[1] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[1] < 4) {
            logger.trace("Only got {} of 4 bytes of header", len[1]);
            return null;
        }

        final byte endian = buf[0];

        /* Parse the variable header length */
        int headerlen;

        if (header == null) {
            headerlen = (int) Message.demarshallint(tbuf, 0, endian, 4);

            /* n % 2^i = n & (2^i - 1) */
            final int modlen = headerlen & 7;

            if (modlen != 0) {
                headerlen += 8 - modlen;
            }
        } else {
            headerlen = header.length - 8;
        }

        /* Read the variable header */
        if (header == null) {
            header = new byte[headerlen + 8];
            System.arraycopy(tbuf, 0, header, 0, 4);
            len[2] = 0;
        }

        if (len[2] < headerlen) {
            try {
                final int rv = inputChannel.read(ByteBuffer.wrap(header, 8 + len[2], headerlen - len[2]));

                if (rv < 0) {
                    throw new EOFException("(3) Underlying transport returned " + rv);
                }

                len[2] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[2] < headerlen) {
            logger.trace("Only got {} of {} bytes of header", len[2], headerlen);
            return null;
        }

        final byte type = buf[1];

        /* Read the body */
        if (body == null) {
            body = new byte[(int) Message.demarshallint(buf, 4, endian, 4)];
            len[3] = 0;
        }

        if (len[3] < body.length) {
            try {
                final int rv = inputChannel.read(ByteBuffer.wrap(body, len[3], body.length - len[3]));

                if (rv < 0) {
                    throw new EOFException("(4) Underlying transport returned " + rv);
                }

                len[3] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[3] < body.length) {
            logger.trace("Only got {} of {} bytes of body", len[3], body.length);
            return null;
        }

        try {
            final Message m = MessageFactory.createMessage(type, buf, header, body, null);
            logger.debug("=> {}", m);

            return m;
        } catch (DBusException | RuntimeException _ex) {
            logger.warn("Exception while creating message.", _ex);

            throw _ex;
        } finally {
            Arrays.fill(tbuf, (byte) 0x00);
            len[1] = 0;
            body = null;
            header = null;
            Arrays.fill(buf, (byte) 0x00);
            len[0] = 0;
        }
    }

    @Override
    public void close() throws IOException {
        if (inputChannel.isOpen()) {
            logger.trace("Closing Message Reader");
            inputChannel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return !inputChannel.isOpen();
    }
}
