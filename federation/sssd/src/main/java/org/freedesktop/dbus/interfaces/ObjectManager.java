package org.freedesktop.dbus.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.DBus.ObjectManager")
@SuppressWarnings({"checkstyle:methodname", "checkstyle:visibilitymodifier"})
public interface ObjectManager extends DBusInterface {
    /**
     * Get a sub-tree of objects. The root of the sub-tree is this object.
     *
     * @return A Map from object path (DBusInterface) to a Map from interface name to a properties Map (as returned by
     *         Properties.GetAll())
     */
    Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects();

    /**
     * Signal generated when a new interface is added
     */
    class InterfacesAdded extends DBusSignal {
        public final DBusPath                             signalSource;
        public final String                               objectPath;

        public final Map<String, Map<String, Variant<?>>> interfaces;

        public InterfacesAdded(String _objectPath, DBusPath _source, Map<String, Map<String, Variant<?>>> _interfaces)
                throws DBusException {
            super(_objectPath, _source, _interfaces);
            objectPath = _objectPath;
            signalSource = _source;
            interfaces = _interfaces;
        }

        /**
         * The source DBus object path (e.g. /org/bluez/hci0/dev_00_11_22_33_44_55).
         *
         * @return DBusPath
         */
        public DBusPath getSignalSource() {
            return signalSource;

        }

        public String getObjectPath() {
            return objectPath;
        }

        /**
         * Returns the added interfaces. Key is a DBus interface name (like org.bluez.Device1). Value is a Map with
         * properties known for the new device.
         *
         * @return Map
         */
        public Map<String, Map<String, Variant<?>>> getInterfaces() {
            return interfaces;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "["
                    + "signalSource=" + signalSource
                    + ", objectPath='" + objectPath + '\''
                    + ", interfaces=" + interfaces
                    + ']';
        }
    }

    /**
     * Signal generated when an interface is removed
     */
    class InterfacesRemoved extends DBusSignal {
        public final DBusPath     signalSource;
        public final String       objectPath;

        public final List<String> interfaces;

        public InterfacesRemoved(String _objectPath, DBusPath _source, List<String> _interfaces)
                throws DBusException {
            super(_objectPath, _source, _interfaces);
            objectPath = _objectPath;
            signalSource = _source;
            interfaces = _interfaces;
        }

        /**
         * The source DBus object path (e.g. /org/bluez/hci0/dev_00_11_22_33_44_55).
         *
         * @return DBusPath
         */
        public DBusPath getSignalSource() {
            return signalSource;
        }

        public String getObjectPath() {
            return objectPath;
        }

        /**
         * Returns list of removed DBus interfaces (like org.bluez.Device1).
         *
         * @return List
         */
        public List<String> getInterfaces() {
            return interfaces;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "["
                    + "signalSource=" + signalSource
                    + ", objectPath='" + objectPath + '\''
                    + ", interfaces=" + interfaces
                    + ']';
        }
    }

}
