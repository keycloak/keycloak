/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.DBus.Description;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * A sample signal with two parameters
 */
@Description("Test interface containing signals")
@DBusInterfaceName("some.other.interface.Name")
public interface TestSignalInterface2 extends DBusInterface {
    @Description("Test basic signal")
    public static class TestRenamedSignal extends DBusSignal {
        public final String value;
        public final UInt32 number;

        /**
         * Create a signal.
         */
        public TestRenamedSignal(String path, String value, UInt32 number) throws DBusException {
            super(path, value, number);
            this.value = value;
            this.number = number;
        }
    }
}
