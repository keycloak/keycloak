package org.freedesktop.dbus.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
* Description of the interface or method, returned in the introspection data
*/
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.Description")
public @interface IntrospectionDescription {
    String value();
}
