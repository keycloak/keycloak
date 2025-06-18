package org.freedesktop.dbus;

/**
 * This class should be extended to create Tuples.
 *
 * Any such class may be used as the return type for a method
 * which returns multiple values.
 *
 * All fields in the Tuple which you wish to be serialized and sent to the
 * remote method should be annotated with the org.freedesktop.dbus.Position
 * annotation, in the order they should appear to DBus.
 */
public abstract class Tuple extends Container {
    public Tuple() {
    }
}
