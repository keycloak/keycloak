package org.freedesktop.dbus.messages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;

public class MethodCall extends MethodBase {
    private static long replyWaitTimeout = Duration.ofSeconds(20).toMillis();

    // CHECKSTYLE:OFF
    Message              reply              = null;
    // CHECKSTYLE:ON

    MethodCall() {
    }

    public MethodCall(String _dest, String _path, String _iface, String _member, byte _flags, String _sig, Object... _args) throws DBusException {
        this(null, _dest, _path, _iface, _member, _flags, _sig, _args);
    }

    public MethodCall(String _source, String _dest, String _path, String _iface, String _member, byte _flags, String _sig, Object... _args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.METHOD_CALL, _flags);

        if (null == _member || null == _path) {
            throw new MessageFormatException("Must specify destination, path and function name to MethodCalls.");
        }
        Object[] header = getHeader();
        header[Message.HeaderField.PATH] = _path;
        header[Message.HeaderField.MEMBER] = _member;

        List<Object> hargs = new ArrayList<>();

        hargs.add(createHeaderArgs(HeaderField.PATH, ArgumentType.OBJECT_PATH_STRING, _path));

        if (null != _source) {
            hargs.add(createHeaderArgs(HeaderField.SENDER, ArgumentType.STRING_STRING, _source));
        }

        if (null != _dest) {
            hargs.add(createHeaderArgs(HeaderField.DESTINATION, ArgumentType.STRING_STRING, _dest));
        }

        if (null != _iface) {
            hargs.add(createHeaderArgs(HeaderField.INTERFACE, ArgumentType.STRING_STRING, _iface));
        }

        hargs.add(createHeaderArgs(HeaderField.MEMBER, ArgumentType.STRING_STRING, _member));

        if (null != _sig) {
            logger.debug("Appending arguments with signature: {}", _sig);
            hargs.add(createHeaderArgs(HeaderField.SIGNATURE, ArgumentType.SIGNATURE_STRING, _sig));
            setArgs(_args);
        }

        appendFileDescriptors(hargs, _sig, _args);
        padAndMarshall(hargs, getSerial(), _sig, _args);
    }

    /**
    * Set the default timeout for method calls.
    * Default is 20s.
    * @param _timeout New timeout in ms.
    */
    public static void setDefaultTimeout(long _timeout) {
        replyWaitTimeout = _timeout;
    }

    public synchronized boolean hasReply() {
        return null != reply;
    }

    /**
    * Block (if necessary) for a reply.
    * @return The reply to this MethodCall, or null if a timeout happens.
    * @param _timeout The length of time to block before timing out (ms).
    */
    public synchronized Message getReply(long _timeout) {
        logger.trace("Blocking on {}", this);
        if (null != reply) {
            return reply;
        }
        try {
            wait(_timeout);
            return reply;
        } catch (InterruptedException _exI) {
            Thread.currentThread().interrupt(); // keep interrupted state
            return reply;
        }
    }

    /**
    * Block (if necessary) for a reply.
    * Default timeout is 20s, or can be configured with setDefaultTimeout()
    * @return The reply to this MethodCall, or null if a timeout happens.
    */
    public synchronized Message getReply() {
        return getReply(replyWaitTimeout);
    }

    public synchronized void setReply(Message _reply) {
        logger.trace("Setting reply to {} to {}", this, _reply);
        this.reply = _reply;
        notifyAll();
    }

}
