package org.freedesktop.dbus.handlers;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;

/**
 * Base class for all signal handling classes.
 * @author hypfvieh
 */
public abstract class AbstractSignalHandlerBase<T extends DBusSignal> implements DBusSigHandler<T> {

    /**
     * Signal-Class which is implemented in subclasses of this class.
     * @return Class
     */
    public abstract Class<T> getImplementationClass();

}
