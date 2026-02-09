package org.freedesktop.dbus.interfaces;

import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.errors.MatchRuleInvalid;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

@SuppressWarnings({"checkstyle:methodname"})
@DBusInterfaceName("org.freedesktop.DBus")
public interface DBus extends DBusInterface {
    int DBUS_NAME_FLAG_ALLOW_REPLACEMENT      = 0x01;
    int DBUS_NAME_FLAG_REPLACE_EXISTING       = 0x02;
    int DBUS_NAME_FLAG_DO_NOT_QUEUE           = 0x04;
    int DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER = 1;
    int DBUS_REQUEST_NAME_REPLY_IN_QUEUE      = 2;
    int DBUS_REQUEST_NAME_REPLY_EXISTS        = 3;
    int DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER = 4;
    int DBUS_RELEASE_NAME_REPLY_RELEASED      = 1;
    int DBUS_RELEASE_NAME_REPLY_NON_EXISTANT  = 2;
    int DBUS_RELEASE_NAME_REPLY_NOT_OWNER     = 3;
    int DBUS_START_REPLY_SUCCESS              = 1;
    int DBUS_START_REPLY_ALREADY_RUNNING      = 2;

    /**
    * Initial message to register ourselves on the Bus.
    * @return The unique name of this connection to the Bus.
    */
    String Hello();

    /**
    * Request a name on the bus.
    * @param _name The name to request.
    * @param _flags DBUS_NAME flags.
    * @return DBUS_REQUEST_NAME_REPLY constants.
    */
    UInt32 RequestName(String _name, UInt32 _flags);

    /**
    * Release a name on the bus.
    * @param _name The name to release.
    * @return DBUS_RELEASE_NAME_REPLY constants.
    */
    UInt32 ReleaseName(String _name);

    /**
    * List the connections currently queued for a name.
    * @param _name The name to query
    * @return A list of unique connection IDs.
    */
    String[] ListQueuedOwners(String _name);

    /**
    * Lists all connected names on the Bus.
    * @return An array of all connected names.
    */
    String[] ListNames();

    /**
     * Returns a list of all names that can be activated on the bus.
     * @return Array of strings where each string is a bus name
     */
    String[] ListActivatableNames();

    /**
    * Determine if a name has an owner.
    * @param _name The name to query.
    * @return true if the name has an owner.
    */
    boolean NameHasOwner(String _name);

    /**
    * Start a service. If the given service is not provided
    * by any application, it will be started according to the .service file
    * for that service.
    * @param _name The service name to start.
    * @param _flags Unused.
    * @return DBUS_START_REPLY constants.
    */
    UInt32 StartServiceByName(String _name, UInt32 _flags);

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * Normally, session bus activated services inherit the environment of the bus daemon. This method adds to or modifies that environment when activating services.
     * Some bus instances, such as the standard system bus, may disable access to this method for some or all callers.
     * Note, both the environment variable names and values must be valid UTF-8. There's no way to update the activation environment with data that is invalid UTF-8.
     *
     * @param _environment Environment to add or update
     */
    void UpdateActivationEnvironment(Map<String, String>[] _environment);

    /**
    * Get the connection unique name that owns the given name.
    * @param _name The name to query.
    * @return The connection which owns the name.
    */
    String GetNameOwner(String _name);

    /**
    * Get the Unix UID that owns a connection name.
    * @param _connectionName The connection name.
    * @return The Unix UID that  it.
    */
    UInt32 GetConnectionUnixUser(String _connectionName);

    /**
    * Returns the process ID associated with a connection.
    * @param _connectionName The name of the connection
    * @return The PID of the connection.
    */
    UInt32 GetConnectionUnixProcessID(String _connectionName);

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * Returns as many credentials as possible for the process connected to
     * the server. If unable to determine certain credentials (for instance,
     * because the process is not on the same machine as the bus daemon,
     * or because this version of the bus daemon does not support a
     * particular security framework), or if the values of those credentials
     * cannot be represented as documented here, then those credentials
     * are omitted.
     * <p>
     * Keys in the returned dictionary not containing "." are defined
     * by this specification. Bus daemon implementors supporting
     * credentials frameworks not mentioned in this document should either
     * contribute patches to this specification, or use keys containing
     * "." and starting with a reversed domain name.
     * </p>
     *
     * @param _busName Unique or well-known bus name of the connection to query, such as :12.34 or com.example.tea
     * @return Credentials
     */
    Map<String, Variant<?>> GetConnectionCredentials(String _busName);

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     *
     * Returns auditing data used by Solaris ADT, in an unspecified<br>
     * binary format. If you know what this means, please contribute<br>
     * documentation via the D-Bus bug tracking system.<br>
     * This method is on the core DBus interface for historical reasons;<br>
     * the same information should be made available via<br>
     * <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#bus-messages-get-connection-credentials">
     * the section called "<code>org.freedesktop.DBus.GetConnectionCredentials</code>"</a><br>
     * in future.<br>
     *
     * @param _busName Unique or well-known bus name of the connection to query, such as :12.34 or com.example.tea
     * @return auditing data as returned by adt_export_session_data()
     */
    Byte[] GetAdtAuditSessionData(String _busName);

    /**
    * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
    * Returns the security context used by SELinux, in an unspecified<br>
    * format. If you know what this means, please contribute<br>
    * documentation via the D-Bus bug tracking system.<br>
    * This method is on the core DBus interface for historical reasons;<br>
    * the same information should be made available via<br>
    * <a  href="https://dbus.freedesktop.org/doc/dbus-specification.html#bus-messages-get-connection-credentials">
    * the section called "<code>org.freedesktop.DBus.GetConnectionCredentials</code>”</a><br>
    * in future.
    *
    * @param _busName Unique or well-known bus name of the connection to query, such as :12.34 or com.example.tea
    *
    * @return some sort of string of bytes, not necessarily UTF-8, not including '\0'
    */
    Byte[] GetConnectionSELinuxSecurityContext(String _busName);

    /**
    * Add a match rule.
    * Will cause you to receive messages that aren't directed to you which
    * match this rule.
    * @param _matchrule The Match rule as a string. Format Undocumented.
    */
    void AddMatch(String _matchrule) throws MatchRuleInvalid;

    /**
    * Remove a match rule.
    * Will cause you to stop receiving messages that aren't directed to you which
    * match this rule.
    * @param _matchrule The Match rule as a string. Format Undocumented.
    */
    void RemoveMatch(String _matchrule) throws MatchRuleInvalid;

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * Gets the unique ID of the bus. The unique ID here is shared among all addresses the<br>
     * bus daemon is listening on (TCP, UNIX domain socket, etc.) and its format is described in<br>
     * <a href="#uuids">the section called "UUIDs”</a>. <br>
     * Each address the bus is listening on also has its own unique<br>
     * ID, as described in <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#addresses">
     * the section called "Server Addresses”</a>. The per-bus and per-address IDs are not related.<br>
     * There is also a per-machine ID, described in
     * <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces-peer">the section called "<code>org.freedesktop.DBus.Peer</code>”</a> and returned
     * by org.freedesktop.DBus.Peer.GetMachineId().<br>
     * For a desktop session bus, the bus ID can be used as a way to uniquely identify a user's session.
     *
     * @return id Unique ID identifying the bus daemon
     */
    String GetId();

    /**
    * Signal sent when the owner of a name changes
    */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    class NameOwnerChanged extends DBusSignal {
        public final String name;
        public final String oldOwner;
        public final String newOwner;

        public NameOwnerChanged(String _path, String _name, String _oldOwner, String _newOwner) throws DBusException {
            super(_path, new Object[] {
                    _name, _oldOwner, _newOwner
            });
            name = _name;
            oldOwner = _oldOwner;
            newOwner = _newOwner;
        }
    }

    /**
    * Signal sent to a connection when it loses a name.
    */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    class NameLost extends DBusSignal {
        public final String name;

        public NameLost(String _path, String _name) throws DBusException {
            super(_path, _name);
            name = _name;
        }
    }

    /**
    * Signal sent to a connection when it acquires a name.
    */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    class NameAcquired extends DBusSignal {
        public final String name;

        public NameAcquired(String _path, String _name) throws DBusException {
            super(_path, _name);
            name = _name;
        }
    }

}
