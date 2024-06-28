package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

import java.io.IOException;

public class MessageTypeException extends IOException implements NonFatalException {
    private static final long serialVersionUID = 935695242304001622L;

    public MessageTypeException(String _message) {
        super(_message);
    }
}
