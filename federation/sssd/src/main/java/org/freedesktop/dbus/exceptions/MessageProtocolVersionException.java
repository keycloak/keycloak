package org.freedesktop.dbus.exceptions;

import java.io.IOException;

import org.freedesktop.dbus.interfaces.FatalException;

public class MessageProtocolVersionException extends IOException implements FatalException {

    private static final long serialVersionUID = 3107039118803575407L;

    public MessageProtocolVersionException(String _message) {
        super(_message);
    }
}
