package org.freedesktop.dbus;

import java.lang.reflect.Type;

import org.freedesktop.dbus.exceptions.DBusException;

public class TypeSignature {
    // CHECKSTYLE:OFF
    String sig;
    // CHECKSTYLE:ON
    public TypeSignature(String _sig) {
        this.sig = _sig;
    }

    public TypeSignature(Type[] _types) throws DBusException {
        StringBuffer sb = new StringBuffer();
        for (Type t : _types) {
            String[] ts = Marshalling.getDBusType(t);
            for (String s : ts) {
                sb.append(s);
            }
        }
        this.sig = sb.toString();
    }

    public String getSig() {
        return sig;
    }
}
