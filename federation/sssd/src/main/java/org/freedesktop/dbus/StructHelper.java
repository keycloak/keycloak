package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.freedesktop.dbus.types.DBusStructType;
import org.freedesktop.dbus.types.Variant;

/**
 * Helper util to create {@link Struct} subclasses when receiving it from DBus.
 *
 * @author David M.
 * @since v3.2.1 - 2019-10-25
 */
public final class StructHelper {

    private StructHelper() {

    }

    /**
     * Creates a instance of the given {@link Struct} subclass if the given variant is some sort of Struct.
     * @param _variant variant to convert
     * @param _structClass {@link Struct} subclass to create
     *
     * @param <T> type of struct
     *
     * @return instance of _structClass or null if _variant is not Struct compatible or any input parameter is null
     *
     * @throws NoSuchMethodException when no constructor can be found for the arguments of the struct
     * @throws SecurityException when constructor cannot be accesses
     * @throws InstantiationException when reflection fails
     * @throws IllegalAccessException  if this Constructor object is enforcing Java language access control and the underlying constructor is inaccessible.
     * @throws IllegalArgumentException when data types are incompatible
     * @throws InvocationTargetException if the underlying constructor throws an exception
     */
    public static <T extends Struct> T createStructFromVariant(Variant<?> _variant, Class<T> _structClass)
            throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (_variant == null || _structClass == null) {
            return null;
        }

        if (_variant.getType() instanceof DBusStructType && _variant.getValue() instanceof Object[]) {
            Class<?>[] argTypes = Arrays.stream((Object[]) _variant.getValue()).map(a -> a.getClass()).toArray(size -> new Class<?>[size]);
            return createStruct(argTypes, _variant.getValue(), _structClass);
        }

        return null;
    }

    /**
     * Will create a new {@link Struct} subclass instance if possible.
     * May replace Wrapper-classes with primitive classes in _constructorArgs if constructor does not match.
     *
     * @param _constructorArgs argument-classes expected by constructor
     * @param _values values passed to the constructor
     * @param _classToConstruct {@link Struct} subclass to instantiate
     *
     * @param <T> type of struct
     *
     * @return instance of _classToConstruct or null if any input argument is null
     *
     * @throws NoSuchMethodException when no constructor can be found for the arguments of the struct
     * @throws SecurityException when constructor cannot be accesses
     * @throws InstantiationException when reflection fails
     * @throws IllegalAccessException  if this Constructor object is enforcing Java language access control and the underlying constructor is inaccessible.
     * @throws IllegalArgumentException when data types are incompatible
     * @throws InvocationTargetException if the underlying constructor throws an exception
     */
    public static <T extends Struct> T createStruct(Class<?>[] _constructorArgs, Object _values,  Class<T> _classToConstruct)
            throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (_constructorArgs == null || _classToConstruct == null || _values == null) {
            return null;
        }

        try {
            Constructor<T> declaredConstructor = _classToConstruct.getDeclaredConstructor(_constructorArgs);
            declaredConstructor.setAccessible(true);
            if (_values instanceof Object[]) {
                return declaredConstructor.newInstance((Object[]) _values);
            } else {
                return declaredConstructor.newInstance(_values);
            }
        } catch (NoSuchMethodException | SecurityException _ex) {
            for (int i = 0; i < _constructorArgs.length; i++) {
                Class<?> class1 = _constructorArgs[i];
                if (ArrayFrob.getWrapperToPrimitiveTypes().containsKey(class1)) {
                    _constructorArgs[i] = ArrayFrob.getWrapperToPrimitiveTypes().get(class1);
                    return createStruct(_constructorArgs, _values, _classToConstruct);
                }
            }
        }
        throw new NoSuchMethodException("Cannot find suitable constructor for arguments " + Arrays.toString(_constructorArgs) + " in class " + _classToConstruct + ".");
    }
}
