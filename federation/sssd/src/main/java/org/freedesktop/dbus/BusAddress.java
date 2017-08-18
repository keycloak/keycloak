/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import cx.ath.matthew.debug.Debug;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static org.freedesktop.dbus.Gettext.getString;

public class BusAddress {
    private String type;
    private Map<String, String> parameters;

    public BusAddress(String address) throws ParseException {
        if (null == address || "".equals(address)) throw new ParseException(getString("busAddressBlank"), 0);
        if (Debug.debug) Debug.print(Debug.VERBOSE, "Parsing bus address: " + address);
        String[] ss = address.split(":", 2);
        if (ss.length < 2) throw new ParseException(getString("busAddressInvalid") + address, 0);
        type = ss[0];
        if (Debug.debug) Debug.print(Debug.VERBOSE, "Transport type: " + type);
        String[] ps = ss[1].split(",");
        parameters = new HashMap<String, String>();
        for (String p : ps) {
            String[] kv = p.split("=", 2);
            parameters.put(kv[0], kv[1]);
        }
        if (Debug.debug) Debug.print(Debug.VERBOSE, "Transport options: " + parameters);
    }

    public String getType() {
        return type;
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String toString() {
        return type + ": " + parameters;
    }
}
