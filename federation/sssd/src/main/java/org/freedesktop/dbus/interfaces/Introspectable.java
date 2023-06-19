package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.annotations.DBusInterfaceName;

/**
* Objects can provide introspection data via this interface and method.
* See the <a href="http://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format">Introspection Format</a>.
*/
@DBusInterfaceName("org.freedesktop.DBus.Introspectable")
public interface Introspectable extends DBusInterface {
    /**
     * @return The XML introspection data for this object
     */
    //CHECKSTYLE:OFF
    String Introspect();
    //CHECKSTYLE:ON
}
