package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>From <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces">DBUS Specification</a>:</b><br>
 * If set to false, the org.freedesktop.DBus.Properties.PropertiesChanged signal,<br>
 * see the section called “org.freedesktop.DBus.Properties” is not guaranteed to be emitted if the property changes.<br>
 * <br>
 * If set to const the property never changes value during the lifetime of the object it belongs to, <br>
 * and hence the signal is never emitted for it. <br>
 * <br>
 * If set to invalidates the signal is emitted but the value is not included in the signal.<br>
 * <br>
 * If set to true the signal is emitted with the value included. <br>
 * The value for the annotation defaults to true if the enclosing interface element does not specify the annotation.
 * Otherwise it defaults to the value specified in the enclosing interface element.<br>
 * <br>
 * This annotation is intended to be used by code generators to implement client-side caching of property values. <br>
 * For all properties for which the annotation is set to const, invalidates or true the client may unconditionally <br>
 * cache the values as the properties don't change or notifications are generated for them if they do.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.Property.EmitsChangedSignal")
public @interface PropertiesEmitsChangedSignal {
    EmitChangeSignal value();

    enum EmitChangeSignal {
        TRUE, INVALIDATES, CONST, FALSE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
