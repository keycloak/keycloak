package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.constants.ArgumentType;
import org.freedesktop.dbus.messages.constants.HeaderField;
import org.freedesktop.dbus.messages.constants.MessageTypes;

public class MethodReturn extends MethodBase {

    private MethodCall call;

    MethodReturn() {
    }

    protected MethodReturn(byte _endianess, String _dest, long _replyserial, String _sig, Object... _args) throws DBusException {
        this(_endianess, null, _dest, _replyserial, _sig, _args);
    }

    protected MethodReturn(byte _endianess, String _source, String _dest, long _replyserial, String _sig, Object... _args) throws DBusException {
        super(_endianess, MessageTypes.METHOD_REPLY.getId(), (byte) 0);

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

        appendFileDescriptors(hargs, _args);
        padAndMarshall(hargs, getSerial(), _sig, _args);
    }

    protected MethodReturn(MethodCall _mc, String _sig, Object... _args) throws DBusException {
        this(null, _mc, _sig, _args);
    }

    protected MethodReturn(String _source, MethodCall _mc, String _sig, Object... _args) throws DBusException {
        this(_mc.getEndianess(), _source, _mc.getSource(), _mc.getSerial(), _sig, _args);
        this.call = _mc;
    }

    public MethodCall getCall() {
        return call;
    }

    public void setCall(MethodCall _call) {
        this.call = _call;
    }
}
