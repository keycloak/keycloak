package org.freedesktop.dbus.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * A standard properties interface.
 */
@DBusInterfaceName("org.freedesktop.DBus.Properties")
@SuppressWarnings({"checkstyle:methodname"})
public interface Properties extends DBusInterface {
    /**
     * Get the value for the given property.
     *
     * @param <A> whatever
     * @param _interfaceName The interface this property is associated with.
     * @param _propertyName The name of the property.
     * @return The value of the property (may be any valid DBus type).
     */
    <A> A Get(String _interfaceName, String _propertyName);

    /**
     * Set the value for the given property.
     *
     * @param <A> whatever
     * @param _interfaceName The interface this property is associated with.
     * @param _propertyName The name of the property.
     * @param _value The new value of the property (may be any valid DBus type).
     */
    <A> void Set(String _interfaceName, String _propertyName, A _value);

    /**
     * Get all properties and values.
     *
     * @param _interfaceName The interface the properties is associated with.
     * @return The properties mapped to their values.
     */
    Map<String, Variant<?>> GetAll(String _interfaceName);

    /**
     * Signal generated when a property changes.
     */
    class PropertiesChanged extends DBusSignal {
        private final Map<String, Variant<?>> propertiesChanged;
        private final List<String>            propertiesRemoved;

        private final String                  interfaceName;

        public PropertiesChanged(String _path, String _interfaceName, Map<String, Variant<?>> _propertiesChanged,
                List<String> _propertiesRemoved) throws DBusException {
            super(_path, _interfaceName, _propertiesChanged, _propertiesRemoved);

            this.propertiesChanged = _propertiesChanged;
            this.propertiesRemoved = _propertiesRemoved;
            this.interfaceName = _interfaceName;
        }

        /**
         * Get name of the interface created this signal (e.g. org.bluez.Adapter1).
         *
         * @return String
         */
        public String getInterfaceName() {
            return interfaceName;
        }

        /**
         * Return the changed properties. Key is the properties name, value is Variant containing any type.
         *
         * @return Map
         */
        public Map<String, Variant<?>> getPropertiesChanged() {
            return propertiesChanged;
        }

        /**
         * Returns a list of removed property keys.
         *
         * @return List
         */
        public List<String> getPropertiesRemoved() {
            return propertiesRemoved;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "["
                    + "propertiesChanged=" + propertiesChanged
                    + ", propertiesRemoved=" + propertiesRemoved
                    + ", interfaceName='" + interfaceName + '\''
                    + ']';
        }
    }
}
