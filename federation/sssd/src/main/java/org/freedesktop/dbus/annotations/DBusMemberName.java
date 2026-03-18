package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Force the member (method/signal) name on the bus to be different to the Java name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.TYPE, ElementType.METHOD
})
public @interface DBusMemberName {
    /** The replacement member name.
     * @return value
     */
    String value();
}
