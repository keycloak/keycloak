package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.annotations.DBusInterfaceName;

/**
 * All DBus Applications should respond to the Ping method on this interface
 */
@DBusInterfaceName("org.freedesktop.DBus.Peer")
@SuppressWarnings({"checkstyle:methodname"})
public interface Peer extends DBusInterface {
    void Ping();

    String GetMachineId();
}
