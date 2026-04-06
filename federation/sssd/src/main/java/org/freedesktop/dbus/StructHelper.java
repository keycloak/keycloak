package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.DBusStructType;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.PrimitiveUtils;

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
     * Creates a {@link ArrayList} of struct of the given type using the list of object arrays.
     *
     * @param _obj list of object arrays to process
     * @param _structType struct class to create
     *
     * @param <T> struct type
     *
     * @return List of given struct type
     *
     * @throws NoSuchMethodException when no constructor can be found for the arguments of the struct
     * @throws SecurityException when constructor cannot be accesses
     * @throws InstantiationException when reflection fails
     * @throws IllegalAccessException  if this Constructor object is enforcing Java language access control and the underlying constructor is inaccessible.
     * @throws IllegalArgumentException when data types are incompatible or incompatible argument length
     * @throws InvocationTargetException if the underlying constructor throws an exception
     *
     * @since 4.3.1 - 2023-08-16
     */
    public static <T extends Struct> List<T> convertToStructList(List<Object[]> _obj, Class<T> _structType) throws NoSuchMethodException, SecurityException, InstantiationException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<T> result = new ArrayList<>();
        convertToStructCollection(_obj, _structType, result);
        return result;
    }

    /**
     * Creates a {@link LinkedHashSet} of struct of the given type using the list of object arrays.
     *
     * @param _obj list of object arrays to process
     * @param _structType struct class to create
     *
     * @param <T> struct type
     *
     * @return List of given struct type
     *
     * @throws NoSuchMethodException when no constructor can be found for the arguments of the struct
     * @throws SecurityException when constructor cannot be accesses
     * @throws InstantiationException when reflection fails
     * @throws IllegalAccessException  if this Constructor object is enforcing Java language access control and the underlying constructor is inaccessible.
     * @throws IllegalArgumentException when data types are incompatible or incompatible argument length
     * @throws InvocationTargetException if the underlying constructor throws an exception
     *
     * @since 4.3.1 - 2023-08-16
     */
    public static <T extends Struct> Set<T> convertToStructSet(Set<Object[]> _obj, Class<T> _structType) throws NoSuchMethodException, SecurityException, InstantiationException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Set<T> result = new LinkedHashSet<>();
        convertToStructCollection(_obj, _structType, result);
        return result;
    }

    /**
     * Creates a collection of struct of the given type using the list of object arrays.
     *
     * @param _input list of object arrays to process
     * @param _structType struct class to create
     * @param _result collection to store results
     *
     * @param <T> struct type
     *
     * @throws NoSuchMethodException when no constructor can be found for the arguments of the struct
     * @throws SecurityException when constructor cannot be accesses
     * @throws InstantiationException when reflection fails
     * @throws IllegalAccessException  if this Constructor object is enforcing Java language access control and the underlying constructor is inaccessible.
     * @throws IllegalArgumentException when data types are incompatible or incompatible argument length
     * @throws InvocationTargetException if the underlying constructor throws an exception
     *
     * @since 4.3.1 - 2023-08-16
     */
    public static <T extends Struct> void convertToStructCollection(Collection<Object[]> _input, Class<T> _structType, Collection<T> _result)
        throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Objects.requireNonNull(_structType, "Struct class required");
        Objects.requireNonNull(_result, "Collection for result storage required");
        Objects.requireNonNull(_input, "Input data required");

        Class<?>[] constructorArgClasses = Arrays.stream(_structType.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Position.class))
            .sorted(Comparator.comparingInt(f -> f.getAnnotation(Position.class).value()))
            .map(Field::getType)
            .toArray(Class[]::new);

        for (Object[] object : _input) {
            if (constructorArgClasses.length != object.length) {
                throw new IllegalArgumentException("Struct length does not match argument length");
            }
            T x = createStruct(constructorArgClasses, (Object) object, _structType);
            _result.add(x);
        }

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
            Class<?>[] argTypes = Arrays.stream((Object[]) _variant.getValue()).map(Object::getClass).toArray(Class<?>[]::new);
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
            if (_values instanceof Object[] oa) {
                return declaredConstructor.newInstance(oa);
            } else {
                return declaredConstructor.newInstance(_values);
            }
        } catch (NoSuchMethodException | SecurityException _ex) {
            for (int i = 0; i < _constructorArgs.length; i++) {
                Class<?> class1 = _constructorArgs[i];
                if (PrimitiveUtils.getWrapperToPrimitiveTypes().containsKey(class1)) {
                    _constructorArgs[i] = PrimitiveUtils.getWrapperToPrimitiveTypes().get(class1);
                    return createStruct(_constructorArgs, _values, _classToConstruct);
                }
            }
        }
        throw new NoSuchMethodException("Cannot find suitable constructor for arguments " + Arrays.toString(_constructorArgs) + " in class " + _classToConstruct + ".");
    }
}
