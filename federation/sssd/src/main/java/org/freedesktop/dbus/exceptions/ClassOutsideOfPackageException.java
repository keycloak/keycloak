package org.freedesktop.dbus.exceptions;

/**
 * @since 5.1.1 - 2024-11-16
 * @author hypfvieh
 */
public class ClassOutsideOfPackageException extends DBusException {
    private static final long serialVersionUID = 1L;

    public ClassOutsideOfPackageException(Class<?> _clz) {
        super("DBusInterfaces cannot be declared outside a package but " + (_clz == null ? null : _clz.getName()) + " has no package");
    }

    public ClassOutsideOfPackageException(String _msg) {
        super(_msg);
    }

}
