package org.freedesktop.dbus.propertyref;

import java.lang.reflect.Method;
import java.util.Objects;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal.EmitChangeSignal;

/**
 * Contains the same information as a {@link DBusBoundProperty}, but as a POJO. Use
 * internally when dealing with properties that are derived from methods annotated
 * with said annotation.
 *
 * @author Brett Smith
 * @since 5.0.0 - 2023-10-20
 */
public final class PropertyRef {

    private final String name;
    private final Class<?> type;
    private final DBusProperty.Access access;
    private final EmitChangeSignal emitChangeSignal;

    public PropertyRef(String _name, Class<?> _type, Access _access) {
        super();
        this.name = _name;
        this.type = _type;
        this.access = _access;
        this.emitChangeSignal = EmitChangeSignal.TRUE;
    }

    public PropertyRef(String _name, Class<?> _type, Access _access, EmitChangeSignal _emitChangeSignal) {
        super();
        this.name = _name;
        this.type = _type;
        this.access = _access;
        this.emitChangeSignal = _emitChangeSignal;
    }

    public PropertyRef(DBusProperty _property) {
        this(_property.name(), _property.type(), _property.access(), _property.emitChangeSignal());
    }

    @Override
    public int hashCode() {
        return Objects.hash(access, name);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        }
        if (_obj == null) {
            return false;
        }
        if (getClass() != _obj.getClass()) {
            return false;
        }
        PropertyRef other = (PropertyRef) _obj;
        return access == other.access && Objects.equals(name, other.name);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public DBusProperty.Access getAccess() {
        return access;
    }

    public EmitChangeSignal getEmitChangeSignal() {
        return emitChangeSignal;
    }

    public static Access accessForMethod(Method _method) {
        DBusBoundProperty annotation = _method.getAnnotation(DBusBoundProperty.class);
        Access access = _method.getName().toLowerCase().startsWith("set") ? Access.WRITE : Access.READ;
        if (annotation.access().equals(Access.READ) || annotation.access().equals(Access.WRITE)) {
            access = annotation.access();
        }
        return access;
    }

    public static Class<?> typeForMethod(Method _method) {
        DBusBoundProperty annotation = _method.getAnnotation(DBusBoundProperty.class);
        Class<?> type = annotation.type();
        if (type == null || type.equals(Void.class)) {
            if (accessForMethod(_method) == Access.READ) {
                return _method.getReturnType();
            } else {
                return _method.getParameterTypes()[0];
            }
        }
        return type;
    }

    public static void checkMethod(Method _method) {
        Access access = accessForMethod(_method);
        if (access == Access.READ && (_method.getParameterCount() > 0 || _method.getReturnType().equals(void.class))) {
            throw new IllegalArgumentException("READ properties must have zero parameters, and not return void.");
        }
        if (access == Access.WRITE && (_method.getParameterCount() != 1 || !_method.getReturnType().equals(void.class))) {
            throw new IllegalArgumentException("WRITE properties must have exactly 1 parameter, and return void.");
        }
    }

}
