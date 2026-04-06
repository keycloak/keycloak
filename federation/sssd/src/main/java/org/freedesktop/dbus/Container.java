package org.freedesktop.dbus;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.freedesktop.dbus.annotations.Position;
import org.slf4j.LoggerFactory;

/**
 * This class is the super class of both Structs and Tuples
 * and holds common methods.
 */
public abstract class Container {
    private static final Map<Type, Type[]> TYPE_CACHE = new ConcurrentHashMap<>();
    private Object[]                       parameters = null;

    Container() {
    }

    private void setup() {
        Field[] fs = getClass().getDeclaredFields();
        Object[] args = new Object[fs.length];

        int diff = 0;
        for (Field f : fs) {
            if (!f.isAnnotationPresent(Position.class)) {
                diff++;
                continue;
            }
            Position p = f.getAnnotation(Position.class);
            f.setAccessible(true);

            try {
                args[p.value()] = f.get(this);
            } catch (IllegalAccessException _exIa) {
                LoggerFactory.getLogger(getClass()).trace("Could not set value", _exIa);
            }
        }

        this.parameters = new Object[args.length - diff];
        System.arraycopy(args, 0, parameters, 0, parameters.length);
    }

    /**
    * Returns the struct contents in order.
    * @return object array
    */
    public final Object[] getParameters() {
        if (null != parameters) {
            return parameters;
        }
        setup();
        return parameters;
    }

    /** Returns this struct as a string. */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("<");
        if (null == parameters) {
            setup();
        }
        if (0 == parameters.length) {
            return sb.append(">").toString();
        }
        sb.append(Arrays.stream(parameters).map(Objects::toString).collect(Collectors.joining(", ")));
        return sb.append(">").toString();
    }

    @Override
    public final boolean equals(Object _other) {
        if (this == _other) {
            return true;
        }
        if (_other == null) {
            return false;
        }

        if (_other instanceof Container cont) {
            return this.getClass().equals(cont.getClass()) && Arrays.equals(this.getParameters(), cont.getParameters());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.deepHashCode(parameters);
        return result;
    }

    static void putTypeCache(Type _k, Type[] _v) {
        TYPE_CACHE.put(_k, _v);
    }

    static Type[] getTypeCache(Type _k) {
        return TYPE_CACHE.get(_k);
    }

}
