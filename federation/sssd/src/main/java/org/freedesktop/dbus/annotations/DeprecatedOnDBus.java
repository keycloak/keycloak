package org.freedesktop.dbus.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that a DBus interface or method is deprecated
 */
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.Deprecated")
public @interface DeprecatedOnDBus {

    /**
     * Annotation value, true by default
     *
     * @return true when the annotated element deprecated
     */
    boolean value() default true;
}
