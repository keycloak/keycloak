package org.freedesktop.dbus;

/**
 * This class should be extended to create Tuples.
 * <p>
 * Any such class may be used as the return type for a method
 * which returns multiple values.
 * </p><p>
 * All fields in the Tuple which you wish to be serialized and sent to the
 * remote method should be annotated with the {@link org.freedesktop.dbus.annotations.Position Position}
 * annotation, in the order they should appear to DBus.
 * </p><p>
 * A Tuple should have generic type parameters, otherwise deserialization may fail.
 * </p>
 * Example class:
 * <pre>
 * public class MyTuple&lt;A, B&gt; extends Tuple {
 *      &#64;Position(0)
 *      private final A firstValue;
 *      &#64;Position(1)
 *      private final B secondValue;
 *
 *      // constructor/getter/setter omitted for brevity
 * }
 * </pre>
 * Example usage:
 * <pre>
 * public SampleDbusInterface extends DBusInterface {
 *     MyTuple&lt;String, Integer&gt; getMyTuple();
 * }
 * </pre>
 */
public abstract class Tuple extends Container {
}
