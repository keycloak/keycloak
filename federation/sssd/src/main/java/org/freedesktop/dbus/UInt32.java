/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.text.MessageFormat;

import static org.freedesktop.dbus.Gettext.getString;

/**
 * Class to represent unsigned 32-bit numbers.
 */
@SuppressWarnings("serial")
public class UInt32 extends Number implements Comparable<UInt32> {
    /**
     * Maximum allowed value
     */
    public static final long MAX_VALUE = 4294967295L;
    /**
     * Minimum allowed value
     */
    public static final long MIN_VALUE = 0;
    private long value;

    /**
     * Create a UInt32 from a long.
     *
     * @param value Must be a valid integer within MIN_VALUE&ndash;MAX_VALUE
     * @throws NumberFormatException if value is not between MIN_VALUE and MAX_VALUE
     */
    public UInt32(long value) {
        if (value < MIN_VALUE || value > MAX_VALUE)
            throw new NumberFormatException(MessageFormat.format(getString("isNotBetween"), new Object[]{value, MIN_VALUE, MAX_VALUE}));
        this.value = value;
    }

    /**
     * Create a UInt32 from a String.
     *
     * @param value Must parse to a valid integer within MIN_VALUE&ndash;MAX_VALUE
     * @throws NumberFormatException if value is not an integer between MIN_VALUE and MAX_VALUE
     */
    public UInt32(String value) {
        this(Long.parseLong(value));
    }

    /**
     * The value of this as a byte.
     */
    public byte byteValue() {
        return (byte) value;
    }

    /**
     * The value of this as a double.
     */
    public double doubleValue() {
        return (double) value;
    }

    /**
     * The value of this as a float.
     */
    public float floatValue() {
        return (float) value;
    }

    /**
     * The value of this as a int.
     */
    public int intValue() {
        return (int) value;
    }

    /**
     * The value of this as a long.
     */
    public long longValue() {
        return /*(long)*/ value;
    }

    /**
     * The value of this as a short.
     */
    public short shortValue() {
        return (short) value;
    }

    /**
     * Test two UInt32s for equality.
     */
    public boolean equals(Object o) {
        return o instanceof UInt32 && ((UInt32) o).value == this.value;
    }

    public int hashCode() {
        return (int) value;
    }

    /**
     * Compare two UInt32s.
     *
     * @return 0 if equal, -ve or +ve if they are different.
     */
    public int compareTo(UInt32 other) {
        return (int) (this.value - other.value);
    }

    /**
     * The value of this as a string
     */
    public String toString() {
        return "" + value;
    }
}
