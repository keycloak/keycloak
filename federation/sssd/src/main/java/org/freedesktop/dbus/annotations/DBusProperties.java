package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for the multiple {@link DBusProperty} annotations in the single class.
 * You probably don't want to use this annotation in your code, please use {@link DBusProperty}.
 *
 * @see DBusProperty
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DBusProperties {

    /**
     * Container for multiple properties
     *
     * @return value
     */
    DBusProperty[] value();
}
