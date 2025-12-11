package org.freedesktop.dbus.connections;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidBusAddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines an address to connect to DBus.
 * The address will define which transport to use.
 */
public class BusAddress {
    private static final Logger       LOGGER     = LoggerFactory.getLogger(BusAddress.class);

    private String                    type;
    private final Map<String, String> parameters = new LinkedHashMap<>();

    /**
     * Creates a new instance from String.
     *
     * @param _address address String
     * @throws DBusException
     *
     * @deprecated Use BusAddress.of instead
     */
    @Deprecated(forRemoval = true, since = "4.2.0 - 2022-07-18")
    public BusAddress(String _address) throws DBusException {
        if (_address == null || _address.isEmpty()) {
            throw new DBusException("Bus address is blank");
        }

        String[] ss = _address.split(":", 2);
        if (ss.length < 2) {
            throw new DBusException("Bus address is invalid: " + _address);
        }

        type = ss[0] != null ? ss[0].toLowerCase(Locale.US) : null;
        if (type == null) {
            throw new DBusException("Unsupported transport type: " + ss[0]);
        }

        String[] ps = ss[1].split(",");
        for (String p : ps) {
            String[] kv = p.split("=", 2);
            parameters.put(kv[0], kv[1]);
        }

    }

    protected BusAddress(BusAddress _obj) {
        if (_obj != null) {
            parameters.putAll(_obj.parameters);
            type = _obj.type;
        }
    }

    /**
     * Creates a copy of the given {@link BusAddress}.
     * If given address is null, an empty {@link BusAddress} object is created.
     *
     * @param _address address to copy
     * @return BusAddress
     * @since 4.2.0 - 2022-07-18
     */
    public static BusAddress of(BusAddress _address) {
        return new BusAddress(_address);
    }

    /**
     * Creates a new {@link BusAddress} from String.
     *
     * @param _address address String, never null or empty
     *
     * @return BusAddress
     * @since 4.2.0 - 2022-07-18
     */
    public static BusAddress of(String _address) {
        if (_address == null || _address.isEmpty()) {
            throw new InvalidBusAddressException("Bus address is blank");
        }

        BusAddress busAddress = new BusAddress((BusAddress) null);

        LOGGER.trace("Parsing bus address: {}", _address);

        String[] ss = _address.split(":", 2);
        if (ss.length < 2) {
            throw new InvalidBusAddressException("Bus address is invalid: " + _address);
        }

        busAddress.type = ss[0] != null ? ss[0].toLowerCase(Locale.US) : null;
        if (busAddress.type == null) {
            throw new InvalidBusAddressException("Unsupported transport type: " + ss[0]);
        }

        LOGGER.trace("Transport type: {}", busAddress.type);

        String[] ps = ss[1].split(",");
        for (String p : ps) {
            String[] kv = p.split("=", 2);
            busAddress.addParameter(kv[0], kv[1]);
        }

        LOGGER.trace("Transport options: {}", busAddress.parameters);

        return busAddress;
    }

    /**
     * Returns the transport type as found in the address.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the transport type in uppercase.
     *
     * @return type
     */
    public String getBusType() {
        return type == null ? null : type.toUpperCase(Locale.US);
    }

    /**
     * Checks if this {@link BusAddress} is for the given bus type.<br>
     * The given type will be compared case-insensitive.
     * <br>
     * e.g.
     * <pre>
     * isBusType("unix");
     * </pre>
     *
     * @param _type to compare
     *
     * @return true if same type (case-insensitive), false if null or not same type
     *
     * @since 4.2.0 - 2022-07-20
     */
    public boolean isBusType(String _type) {
        return type != null && type.equalsIgnoreCase(_type);
    }

    /**
     * True if this is a listening address.
     * @return true if listening
     */
    public boolean isListeningSocket() {
        return parameters.containsKey("listen");
    }

    public String getGuid() {
        return parameters.get("guid");
    }

    /**
     * String version of the BusAddress.
     * @return String
     *
     * @deprecated use {@link #toString()}
     */
    @Deprecated(forRemoval = true, since = "4.2.0 - 2022-07-18")
    public String getRawAddress() {
        return toString();
    }

    @Override
    public final String toString() {
        return type + ":" + parameters.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
    }

    /**
     * True if this address represents a listening server address.
     * @return true if server
     */
    public boolean isServer() {
        return isListeningSocket();
    }

    /**
     * Add a parameter to the address.
     * Adding multiple parameters with same name is not possible and will overwrite previous values.
     *
     * @param _parameter parameter name
     * @param _value value
     *
     * @return this
     * @since 4.2.0 - 2022-07-18
     */
    public BusAddress addParameter(String _parameter, String _value) {
        parameters.put(_parameter, _value);
        return this;
    }

    /**
     * Remove parameter with given name.
     * If parameter does not exists, nothing will happen.
     *
     * @param _parameter parameter to remove
     *
     * @return this
     * @since 4.2.0 - 2022-07-18
     */
    public BusAddress removeParameter(String _parameter) {
        parameters.remove(_parameter);
        return this;
    }

    /**
     * Checks if the given parameter is present.
     *
     * @param _parameter parameter to check
     *
     * @return true if parameter exists, false otherwise
     * @since 4.2.2 - 2023-01-11
     */
    public boolean hasParameter(String _parameter) {
        return parameters.containsKey(_parameter);
    }

    /**
     * Returns a read-only view of the parameters currently configured.
     *
     * @return Map, maybe empty
     * @since 4.2.0 - 2022-07-18
     *
     * @deprecated will be removed in future, use {@link #getParameterValue(String)} or {@link #hasParameter(String)}
     */
    @Deprecated(forRemoval = true, since = "4.2.2 - 2023-01-11")
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Returns a the value of the given parameter.
     * <p>
     * When no value present, <code>null</code> is returned.
     *
     * @param _parameter parameter to get value for
     *
     * @return String or <code>null</code>
     * @since 4.2.0 - 2022-07-19
     */
    public String getParameterValue(String _parameter) {
        return parameters.get(_parameter);
    }

    /**
     * Returns a the value of the given parameter.
     * <p>
     * When no value present, the given default is returned.
     *
     * @param _parameter parameter to get value for
     * @param _default default to return if parameter not set
     *
     * @return String or default
     * @since 4.2.2 - 2023-01-11
     */
    public String getParameterValue(String _parameter, String _default) {
        return parameters.getOrDefault(_parameter, _default);
    }

    /**
     * Creates a listening BusAddress if this instance is not already listening.
     *
     * @return new BusAddress or this
     * @since 4.2.0 - 2022-07-18
     */
    public BusAddress getListenerAddress() {
        if (!isListeningSocket()) {
            return new BusAddress(this).addParameter("listen", "true");
        }
        return this;
    }

}
