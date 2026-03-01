package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods annotated with this do not send a reply
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.Method.NoReply")
public @interface MethodNoReply {

    /**
     * Annotation value, true by default
     *
     * @return true when a method doesn't send a reply
     */
    boolean value() default true;
}
