package org.freedesktop.dbus.types;

/**
 * Class to represent unsigned 32-bit numbers.
 */
@SuppressWarnings("serial")
public class UInt32 extends Number implements Comparable<UInt32> {
    /** Maximum allowed value */
    public static final long MAX_VALUE = 4294967295L;
    /** Minimum allowed value */
    public static final long MIN_VALUE = 0;
    private final long value;

    /** Create a UInt32 from a long.
    * @param _value Must be a valid integer within MIN_VALUE&ndash;MAX_VALUE
    * @throws NumberFormatException if value is not between MIN_VALUE and MAX_VALUE
    */
    public UInt32(long _value) {
        if (_value < MIN_VALUE || _value > MAX_VALUE) {
            throw new NumberFormatException(String.format("%s is not between %s and %s.", _value, MIN_VALUE, MAX_VALUE));
        }
        value = _value;
    }

    /** Create a UInt32 from a String.
    * @param _value Must parse to a valid integer within MIN_VALUE&ndash;MAX_VALUE
    * @throws NumberFormatException if value is not an integer between MIN_VALUE and MAX_VALUE
    */
    public UInt32(String _value) {
        this(Long.parseLong(_value));
    }

    /** The value of this as a byte. */
    @Override
    public byte byteValue() {
        return (byte) value;
    }

    /** The value of this as a double. */
    @Override
    public double doubleValue() {
        return value;
    }

    /** The value of this as a float. */
    @Override
    public float floatValue() {
        return value;
    }

    /** The value of this as a int. */
    @Override
    public int intValue() {
        return (int) value;
    }

    /** The value of this as a long. */
    @Override
    public long longValue() {
        return /*(long)*/ value;
    }

    /** The value of this as a short. */
    @Override
    public short shortValue() {
        return (short) value;
    }

    /** Test two UInt32s for equality. */
    @Override
    public boolean equals(Object _o) {
        return _o instanceof UInt32 && ((UInt32) _o).value == this.value;
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    /** Compare two UInt32s.
    * @return 0 if equal, -ve or +ve if they are different.
    */
    @Override
    public int compareTo(UInt32 _other) {
        return Long.compare(value, _other.value);
    }

    /** The value of this as a string */
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
