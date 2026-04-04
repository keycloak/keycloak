package org.freedesktop.dbus.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.freedesktop.dbus.config.DBusSysProps;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.AddressResolvingException;

public final class AddressBuilder {
    /**
     * @deprecated Constant has been moved to {@link DBusSysProps}.
     */
    @Deprecated(forRemoval = true, since = "v4.2.2 - 2023-01-20")
    public static final String  DBUS_SYSTEM_BUS_ADDRESS        = "DBUS_SYSTEM_BUS_ADDRESS";
    /**
     * @deprecated Constant has been moved to {@link DBusSysProps}.
     */
    @Deprecated(forRemoval = true, since = "v4.2.2 - 2023-01-20")
    public static final String  DEFAULT_SYSTEM_BUS_ADDRESS     = "unix:path=/var/run/dbus/system_bus_socket";
    /**
     * @deprecated Constant has been moved to {@link DBusSysProps}.
     */
    @Deprecated(forRemoval = true, since = "v4.2.2 - 2023-01-20")
    public static final String  DBUS_SESSION_BUS_ADDRESS       = "DBUS_SESSION_BUS_ADDRESS";

    private AddressBuilder() {}

    /**
     * Determine the address of the DBus system connection.
     *
     * @return String
     */
    public static BusAddress getSystemConnection() {
        String bus = System.getenv(DBusSysProps.DBUS_SYSTEM_BUS_ADDRESS);
        if (bus == null) {
            bus = DBusSysProps.DEFAULT_SYSTEM_BUS_ADDRESS;
        }
        return BusAddress.of(bus);
    }

    /**
     * Retrieves the connection address to connect to the DBus session-bus.<br>
     * Will return TCP connection when no unix transport found and TCP is available.
     *
     * @param _dbusMachineIdFile alternative location of dbus machine id file, use null if not needed
     *
     * @return address
     *
     * @throws AddressResolvingException when no suitable address can be found for any available transport
     */
    public static BusAddress getSessionConnection(String _dbusMachineIdFile) {

        // try to read session address from running process instance properties first
        String s = System.getProperty(DBusSysProps.DBUS_SESSION_BUS_ADDRESS);

        // no session address in process properties, try to get it from environment
        if (s == null) {
            // MacOS support: e.g DBUS_LAUNCHD_SESSION_BUS_SOCKET=/private/tmp/com.apple.launchd.4ojrKe6laI/unix_domain_listener
            if (Util.isMacOs()) {
                s = "unix:path=" + System.getenv(DBusSysProps.DBUS_SESSION_BUS_ADDRESS_MACOS);
            } else { // all others (linux)
                s = System.getenv(DBusSysProps.DBUS_SESSION_BUS_ADDRESS);
            }
        }

        // no address found in instance properties and environment, try to get the address from session properties file
        if (s == null) {
            // address gets stashed in $HOME/.dbus/session-bus/`dbus-uuidgen --get`-`sed 's/:\(.\)\..*/\1/' <<<
            // $DISPLAY`
            String display = System.getenv("DISPLAY");
            if (display == null) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address: DISPLAY variable not set");
            }
            if (display.charAt(0) != ':' && display.contains(":")) { // display seems to be a remote display
                                                                     // (e.g. X forward through SSH)
                display = display.substring(display.indexOf(':'));
            }

            String uuid = getDbusMachineId(_dbusMachineIdFile);
            String homedir = System.getProperty("user.home");
            File addressfile = new File(homedir + "/.dbus/session-bus",
                    uuid + "-" + display.replaceAll(":([0-9]*)\\..*", "$1"));

            if (!addressfile.exists()) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address: " + addressfile + " not found");
            }

            Properties readProperties = Util.readProperties(addressfile);
            if (readProperties == null) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address: Unable to read " + addressfile);
            }
            String sessionAddress = readProperties.getProperty(DBusSysProps.DBUS_SESSION_BUS_ADDRESS);

            if (Util.isEmpty(sessionAddress)) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address: No session information found in " + addressfile);
            }

            // sometimes (e.g. Ubuntu 18.04) the returned address is wrapped in single quotes ('), we have to remove them
            if (sessionAddress.matches("^'[^']+'$")) {
                sessionAddress = sessionAddress.replaceFirst("^'([^']+)'$", "$1");
            }

            return BusAddress.of(sessionAddress);
        }

        return BusAddress.of(s);
    }

    /**
     * Extracts the machine-id usually found on Linux in various system directories, or
     * generate a fake id for non-Linux platforms.
     *
     * @param _dbusMachineIdFile alternative location of dbus machine id file, null if not needed
     * @return machine-id string, never null
     */
    public static String getDbusMachineId(String _dbusMachineIdFile) {
        File uuidfile = determineMachineIdFile(_dbusMachineIdFile);
        if (uuidfile != null) {
            String uuid = Util.readFileToString(uuidfile);
            if (uuid.length() > 0) {
                return uuid;
            } else {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address: MachineId file is empty.");
            }
        }
        if (Util.isWindows() || Util.isMacOs()) {
            /* Linux *should* have a machine-id */
            return getFakeDbusMachineId();
        }
        throw new AddressResolvingException("Cannot Resolve Session Bus Address: MachineId file can not be found");
    }

    /**
     * Tries to find the DBus machine-id file in different locations.
     *
     * @param _dbusMachineIdFile alternative location of dbus machine id file
     *
     * @return File with machine-id
     */
    private static File determineMachineIdFile(String _dbusMachineIdFile) {
        List<String> locationPriorityList = Arrays.asList(System.getenv(DBusSysProps.DBUS_MACHINE_ID_SYS_VAR), _dbusMachineIdFile,
                "/var/lib/dbus/machine-id", "/usr/local/var/lib/dbus/machine-id", "/etc/machine-id");
        return locationPriorityList.stream()
                .filter(s -> s != null)
                .map(s -> new File(s))
                .filter(f -> f.exists() && f.length() > 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * Generates a fake machine-id when DBus is running on Windows.
     * @return String
     */
    private static String getFakeDbusMachineId() {
        // we create a fake id on windows
        return String.format("%s@%s", Util.getCurrentUser(), Util.getHostName());
    }

}
