package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.FatalException;

import java.io.IOException;

public class MessageProtocolVersionException extends IOException implements FatalException {

    private static final long serialVersionUID = 3107039118803575407L;

    public MessageProtocolVersionException(String _message) {
        super(_message);
    }
}
