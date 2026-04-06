package org.freedesktop.dbus.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Pattern;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;

/**
 * DBus name Util class for internal and external use.
 */
public final class DBusNamingUtil {
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("[$]");

    private DBusNamingUtil() {
    }

    /**
     * Get DBus interface name for specified interface class
     *
     * @param _clazz input DBus interface class
     * @return interface name
     * @see DBusInterfaceName
     */
    public static String getInterfaceName(Class<?> _clazz) {
        Objects.requireNonNull(_clazz, "Class must not be null");

        if (_clazz.isAnnotationPresent(DBusInterfaceName.class)) {
            return _clazz.getAnnotation(DBusInterfaceName.class).value();
        }
        return DOLLAR_PATTERN.matcher(_clazz.getName()).replaceAll(".");
    }

    /**
     * Get DBus method name for specified method object.
     *
     * @param _method input method
     * @return method name
     * @see DBusMemberName
     */
    public static String getMethodName(Method _method) {
        Objects.requireNonNull(_method, "method must not be null");

        if (_method.isAnnotationPresent(DBusMemberName.class)) {
            return _method.getAnnotation(DBusMemberName.class).value();
        }
        return _method.getName();
    }

    /**
     * Get a property name for a method (annotated with {@link DBusBoundProperty}. These
     * would typically be setter / getter type methods. If {@link DBusBoundProperty#name()} is
     * provided, that will take precedence.
     *
     * @param _method input method
     * @return property name
     * @see DBusMemberName
     */
    public static String getPropertyName(Method _method) {
        Objects.requireNonNull(_method, "method must not be null");

        if (_method.isAnnotationPresent(DBusBoundProperty.class)) {
            String defName = _method.getAnnotation(DBusBoundProperty.class).name();
            if (!"".equals(defName)) {
                return defName;
            }
        }
        String name = _method.getName();
        String lowerCaseName = name.toLowerCase();
        if ((lowerCaseName.startsWith("get") && !"get".equals(lowerCaseName))
         || (lowerCaseName.startsWith("set") && !"set".equals(lowerCaseName))) {
            name = name.substring(3);
        } else if (lowerCaseName.startsWith("is") && !"is".equals(lowerCaseName)) {
            name = name.substring(2);
        }
        return name;
    }

    /**
     * Get DBus signal name for specified signal class.
     *
     * @param _clazz input DBus signal class
     * @return signal name
     * @see DBusMemberName
     */
    public static String getSignalName(Class<?> _clazz) {
        Objects.requireNonNull(_clazz, "Class must not be null");

        if (_clazz.isAnnotationPresent(DBusMemberName.class)) {
            return _clazz.getAnnotation(DBusMemberName.class).value();
        }
        return _clazz.getSimpleName();
    }

    /**
     * Get DBus name for specified annotation class
     *
     * @param _clazz input DBus annotation
     * @return interface name
     * @see DBusInterfaceName
     */
    public static String getAnnotationName(Class<? extends Annotation> _clazz) {
        Objects.requireNonNull(_clazz, "Class must not be null");

        if (_clazz.isAnnotationPresent(DBusInterfaceName.class)) {
            return _clazz.getAnnotation(DBusInterfaceName.class).value();
        }
        return DOLLAR_PATTERN.matcher(_clazz.getName()).replaceAll(".");
    }
}
