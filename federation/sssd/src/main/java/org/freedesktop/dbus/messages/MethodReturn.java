package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

public class MethodReturn extends MethodBase {

    private MethodCall call;

    MethodReturn() {
    }

    public MethodReturn(String _dest, long _replyserial, String _sig, Object... _args) throws DBusException {
        this(null, _dest, _replyserial, _sig, _args);
    }

    public MethodReturn(String _source, String _dest, long _replyserial, String _sig, Object... _args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.METHOD_RETURN, (byte) 0);

        List<Object> hargs = new ArrayList<>();
        hargs.add(createHeaderArgs(HeaderField.REPLY_SERIAL, ArgumentType.UINT32_STRING, _replyserial));

        if (null != _source) {
            hargs.add(createHeaderArgs(HeaderField.SENDER, ArgumentType.STRING_STRING, _source));
        }

        if (null != _dest) {
            hargs.add(createHeaderArgs(HeaderField.DESTINATION, ArgumentType.STRING_STRING, _dest));
        }

        if (null != _sig) {
            hargs.add(createHeaderArgs(HeaderField.SIGNATURE, ArgumentType.SIGNATURE_STRING, _sig));
            setArgs(_args);
        }

        appendFileDescriptors(hargs, _sig, _args);
        padAndMarshall(hargs, getSerial(), _sig, _args);
    }

    public MethodReturn(MethodCall _mc, String _sig, Object... _args) throws DBusException {
        this(null, _mc, _sig, _args);
    }

    public MethodReturn(String _source, MethodCall _mc, String _sig, Object... _args) throws DBusException {
        this(_source, _mc.getSource(), _mc.getSerial(), _sig, _args);
        this.call = _mc;
    }

    public MethodCall getCall() {
        return call;
    }

    public void setCall(MethodCall _call) {
        this.call = _call;
    }
}
