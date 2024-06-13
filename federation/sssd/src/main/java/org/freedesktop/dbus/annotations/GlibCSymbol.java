package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a C symbol to map to this method. Used by GLib only
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.GLib.CSymbol")
public @interface GlibCSymbol {
    String value();
}
