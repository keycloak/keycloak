package org.freedesktop.dbus.config;

/**
 * Constant class containing all properties supported as system properties.
 *
 * @author hypfvieh
 * @since v4.2.2 - 2023-01-20
 */
public final class DBusSysProps {
    public static final String SYSPROP_DBUS_TEST_HOME_DIR     = "DBUS_TEST_HOMEDIR";

    public static final String DBUS_SYSTEM_BUS_ADDRESS        = "DBUS_SYSTEM_BUS_ADDRESS";
    public static final String DEFAULT_SYSTEM_BUS_ADDRESS     = "unix:path=/var/run/dbus/system_bus_socket";
    public static final String DBUS_SESSION_BUS_ADDRESS       = "DBUS_SESSION_BUS_ADDRESS";

    public static final String DBUS_MACHINE_ID_SYS_VAR        = "DBUS_MACHINE_ID_LOCATION";

    public static final String DBUS_SESSION_BUS_ADDRESS_MACOS = "DBUS_LAUNCHD_SESSION_BUS_SOCKET";

    private DBusSysProps() {}
}
