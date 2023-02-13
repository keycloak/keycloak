/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

public interface DBus extends DBusInterface {

    String BUSNAME = "org.freedesktop.DBus";
    String OBJECTPATH = "/org/freedesktop/DBus";

    int DBUS_NAME_FLAG_ALLOW_REPLACEMENT = 0x01;
    int DBUS_NAME_FLAG_REPLACE_EXISTING = 0x02;
    int DBUS_NAME_FLAG_DO_NOT_QUEUE = 0x04;
    int DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER = 1;
    int DBUS_REQUEST_NAME_REPLY_IN_QUEUE = 2;
    int DBUS_REQUEST_NAME_REPLY_EXISTS = 3;
    int DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER = 4;
    int DBUS_RELEASEME_REPLY_RELEASED = 1;
    int DBUS_RELEASE_NAME_REPLY_NON_EXISTANT = 2;
    int DBUS_RELEASE_NAME_REPLY_NOT_OWNER = 3;
    int DBUS_START_REPLY_SUCCESS = 1;
    int DBUS_START_REPLY_ALREADY_RUNNING = 2;

    /**
     * All DBus Applications should respond to the Ping method on this interface
     */
    public interface Peer extends DBusInterface {
        public void Ping();
    }

    /**
     * Objects can provide introspection data via this interface and method.
     * See the <a href="http://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format">Introspection Format</a>.
     */
    public interface Introspectable extends DBusInterface {
        /**
         * @return The XML introspection data for this object
         */
        public String Introspect();
    }

    /**
     * A standard properties interface.
     */
    public interface Properties extends DBusInterface {
        /**
         * Get the value for the given property.
         *
         * @param interface_name The interface this property is associated with.
         * @param property_name  The name of the property.
         * @return The value of the property (may be any valid DBus type).
         */
        public <A> A Get(String interface_name, String property_name);

        /**
         * Set the value for the given property.
         *
         * @param interface_name The interface this property is associated with.
         * @param property_name  The name of the property.
         * @param value          The new value of the property (may be any valid DBus type).
         */
        public <A> void Set(String interface_name, String property_name, A value);

        /**
         * Get all properties and values.
         *
         * @param interface_name The interface the properties is associated with.
         * @return The properties mapped to their values.
         */
        public Map<String, Variant> GetAll(String interface_name);
    }

    /**
     * Messages generated locally in the application.
     */
    public interface Local extends DBusInterface {
        public class Disconnected extends DBusSignal {
            public Disconnected(String path) throws DBusException {
                super(path);
            }
        }
    }

    /**
     * Initial message to register ourselves on the Bus.
     *
     * @return The unique name of this connection to the Bus.
     */
    public String Hello();

    /**
     * Lists all connected names on the Bus.
     *
     * @return An array of all connected names.
     */
    public String[] ListNames();

    /**
     * Determine if a name has an owner.
     *
     * @param name The name to query.
     * @return true if the name has an owner.
     */
    public boolean NameHasOwner(String name);

    /**
     * Get the connection unique name that owns the given name.
     *
     * @param name The name to query.
     * @return The connection which owns the name.
     */
    public String GetNameOwner(String name);

    /**
     * Get the Unix UID that owns a connection name.
     *
     * @param connection_name The connection name.
     * @return The Unix UID that owns it.
     */
    public UInt32 GetConnectionUnixUser(String connection_name);

    /**
     * Start a service. If the given service is not provided
     * by any application, it will be started according to the .service file
     * for that service.
     *
     * @param name  The service name to start.
     * @param flags Unused.
     * @return DBUS_START_REPLY constants.
     */
    public UInt32 StartServiceByName(String name, UInt32 flags);

    /**
     * Request a name on the bus.
     *
     * @param name  The name to request.
     * @param flags DBUS_NAME flags.
     * @return DBUS_REQUEST_NAME_REPLY constants.
     */
    public UInt32 RequestName(String name, UInt32 flags);

    /**
     * Release a name on the bus.
     *
     * @param name The name to release.
     * @return DBUS_RELEASE_NAME_REPLY constants.
     */
    public UInt32 ReleaseName(String name);

    /**
     * Add a match rule.
     * Will cause you to receive messages that aren't directed to you which
     * match this rule.
     *
     * @param matchrule The Match rule as a string. Format Undocumented.
     */
    public void AddMatch(String matchrule) throws Error.MatchRuleInvalid;

    /**
     * Remove a match rule.
     * Will cause you to stop receiving messages that aren't directed to you which
     * match this rule.
     *
     * @param matchrule The Match rule as a string. Format Undocumented.
     */
    public void RemoveMatch(String matchrule) throws Error.MatchRuleInvalid;

    /**
     * List the connections currently queued for a name.
     *
     * @param name The name to query
     * @return A list of unique connection IDs.
     */
    public String[] ListQueuedOwners(String name);

    /**
     * Returns the proccess ID associated with a connection.
     *
     * @param connection_name The name of the connection
     * @return The PID of the connection.
     */
    public UInt32 GetConnectionUnixProcessID(String connection_name);

    /**
     * Does something undocumented.
     */
    public Byte[] GetConnectionSELinuxSecurityContext(String a);

    /**
     * Does something undocumented.
     */
    public void ReloadConfig();

    /**
     * Signal sent when the owner of a name changes
     */
    public class NameOwnerChanged extends DBusSignal {
        public final String name;
        public final String old_owner;
        public final String new_owner;

        public NameOwnerChanged(String path, String name, String old_owner, String new_owner) throws DBusException {
            super(path, new Object[]{name, old_owner, new_owner});
            this.name = name;
            this.old_owner = old_owner;
            this.new_owner = new_owner;
        }
    }

    /**
     * Signal sent to a connection when it loses a name
     */
    public class NameLost extends DBusSignal {
        public final String name;

        public NameLost(String path, String name) throws DBusException {
            super(path, name);
            this.name = name;
        }
    }

    /**
     * Signal sent to a connection when it aquires a name
     */
    public class NameAcquired extends DBusSignal {
        public final String name;

        public NameAcquired(String path, String name) throws DBusException {
            super(path, name);
            this.name = name;
        }
    }

    /**
     * Contains standard errors that can be thrown from methods.
     */
    public interface Error {
        /**
         * Thrown if the method called was unknown on the remote object
         */
        @SuppressWarnings("serial")
        public class UnknownMethod extends DBusExecutionException {
            public UnknownMethod(String message) {
                super(message);
            }
        }

        /**
         * Thrown if the object was unknown on a remote connection
         */
        @SuppressWarnings("serial")
        public class UnknownObject extends DBusExecutionException {
            public UnknownObject(String message) {
                super(message);
            }
        }

        /**
         * Thrown if the requested service was not available
         */
        @SuppressWarnings("serial")
        public class ServiceUnknown extends DBusExecutionException {
            public ServiceUnknown(String message) {
                super(message);
            }
        }

        /**
         * Thrown if the match rule is invalid
         */
        @SuppressWarnings("serial")
        public class MatchRuleInvalid extends DBusExecutionException {
            public MatchRuleInvalid(String message) {
                super(message);
            }
        }

        /**
         * Thrown if there is no reply to a method call
         */
        @SuppressWarnings("serial")
        public class NoReply extends DBusExecutionException {
            public NoReply(String message) {
                super(message);
            }
        }

        /**
         * Thrown if a message is denied due to a security policy
         */
        @SuppressWarnings("serial")
        public class AccessDenied extends DBusExecutionException {
            public AccessDenied(String message) {
                super(message);
            }
        }
    }

    /**
     * Description of the interface or method, returned in the introspection data
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Description {
        String value();
    }

    /**
     * Indicates that a DBus interface or method is deprecated
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Deprecated {
    }

    /**
     * Contains method-specific annotations
     */
    public interface Method {
        /**
         * Methods annotated with this do not send a reply
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface NoReply {
        }

        /**
         * Give an error that the method can return
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface Error {
            String value();
        }
    }

    /**
     * Contains GLib-specific annotations
     */
    public interface GLib {
        /**
         * Define a C symbol to map to this method. Used by GLib only
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface CSymbol {
            String value();
        }
    }

    /**
     * Contains Binding-test interfaces
     */
    public interface Binding {
        public interface SingleTests extends DBusInterface {
            @Description("Returns the sum of the values in the input list")
            public UInt32 Sum(byte[] a);
        }

        public interface TestClient extends DBusInterface {
            @Description("when the trigger signal is received, this method should be called on the sending process/object.")
            public void Response(UInt16 a, double b);

            @Description("Causes a callback")
            public static class Trigger extends DBusSignal {
                public final UInt16 a;
                public final double b;

                public Trigger(String path, UInt16 a, double b) throws DBusException {
                    super(path, a, b);
                    this.a = a;
                    this.b = b;
                }
            }

        }

        public interface Tests extends DBusInterface {
            @Description("Returns whatever it is passed")
            public <T> Variant<T> Identity(Variant<T> input);

            @Description("Returns whatever it is passed")
            public byte IdentityByte(byte input);

            @Description("Returns whatever it is passed")
            public boolean IdentityBool(boolean input);

            @Description("Returns whatever it is passed")
            public short IdentityInt16(short input);

            @Description("Returns whatever it is passed")
            public UInt16 IdentityUInt16(UInt16 input);

            @Description("Returns whatever it is passed")
            public int IdentityInt32(int input);

            @Description("Returns whatever it is passed")
            public UInt32 IdentityUInt32(UInt32 input);

            @Description("Returns whatever it is passed")
            public long IdentityInt64(long input);

            @Description("Returns whatever it is passed")
            public UInt64 IdentityUInt64(UInt64 input);

            @Description("Returns whatever it is passed")
            public double IdentityDouble(double input);

            @Description("Returns whatever it is passed")
            public String IdentityString(String input);

            @Description("Returns whatever it is passed")
            public <T> Variant<T>[] IdentityArray(Variant<T>[] input);

            @Description("Returns whatever it is passed")
            public byte[] IdentityByteArray(byte[] input);

            @Description("Returns whatever it is passed")
            public boolean[] IdentityBoolArray(boolean[] input);

            @Description("Returns whatever it is passed")
            public short[] IdentityInt16Array(short[] input);

            @Description("Returns whatever it is passed")
            public UInt16[] IdentityUInt16Array(UInt16[] input);

            @Description("Returns whatever it is passed")
            public int[] IdentityInt32Array(int[] input);

            @Description("Returns whatever it is passed")
            public UInt32[] IdentityUInt32Array(UInt32[] input);

            @Description("Returns whatever it is passed")
            public long[] IdentityInt64Array(long[] input);

            @Description("Returns whatever it is passed")
            public UInt64[] IdentityUInt64Array(UInt64[] input);

            @Description("Returns whatever it is passed")
            public double[] IdentityDoubleArray(double[] input);

            @Description("Returns whatever it is passed")
            public String[] IdentityStringArray(String[] input);

            @Description("Returns the sum of the values in the input list")
            public long Sum(int[] a);

            @Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
            public Map<String, List<String>> InvertMapping(Map<String, String> a);

            @Description("This method returns the contents of a struct as separate values")
            public Triplet<String, UInt32, Short> DeStruct(TestStruct a);

            @Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
            public List<Variant<Object>> Primitize(Variant<Object> a);

            @Description("inverts it's input")
            public boolean Invert(boolean a);

            @Description("triggers sending of a signal from the supplied object with the given parameter")
            public void Trigger(String a, UInt64 b);

            @Description("Causes the server to exit")
            public void Exit();
        }

        public interface TestSignals extends DBusInterface {
            @Description("Sent in response to a method call")
            public static class Triggered extends DBusSignal {
                public final UInt64 a;

                public Triggered(String path, UInt64 a) throws DBusException {
                    super(path, a);
                    this.a = a;
                }
            }
        }

        public final class Triplet<A, B, C> extends Tuple {
            @Position(0)
            public final A a;
            @Position(1)
            public final B b;
            @Position(2)
            public final C c;

            public Triplet(A a, B b, C c) {
                this.a = a;
                this.b = b;
                this.c = c;
            }
        }

        public final class TestStruct extends Struct {
            @Position(0)
            public final String a;
            @Position(1)
            public final UInt32 b;
            @Position(2)
            public final Short c;

            public TestStruct(String a, UInt32 b, Short c) {
                this.a = a;
                this.b = b;
                this.c = c;
            }
        }
    }
}