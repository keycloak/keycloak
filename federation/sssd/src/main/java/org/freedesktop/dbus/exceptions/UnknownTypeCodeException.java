package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

public class UnknownTypeCodeException extends DBusException implements NonFatalException {
    private static final long serialVersionUID = -4688075573912580455L;

    public UnknownTypeCodeException(byte _code) {
        super("Not a valid D-Bus type code: " + _code);
    }
}
