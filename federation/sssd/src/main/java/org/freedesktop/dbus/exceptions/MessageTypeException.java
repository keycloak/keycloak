package org.freedesktop.dbus.exceptions;

import java.io.IOException;

import org.freedesktop.dbus.interfaces.NonFatalException;

public class MessageTypeException extends IOException implements NonFatalException {
    private static final long serialVersionUID = 935695242304001622L;

    public MessageTypeException(String _message) {
        super(_message);
    }
}
