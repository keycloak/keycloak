package org.freedesktop.dbus.handlers;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;

/**
 * Subclass this abstract class for creating a callback for changed properties.
 *
 * As soon as your callback is registered by calling {@link AbstractConnection#addSigHandler(Class, DBusSigHandler)},
 * all property changes by Dbus will be visible in the handle(DBusSigHandler) method of your callback class.
 */
public abstract class AbstractPropertiesChangedHandler extends AbstractSignalHandlerBase<Properties.PropertiesChanged> {

    @Override
    public final Class<PropertiesChanged> getImplementationClass() {
        return Properties.PropertiesChanged.class;
    }

}
