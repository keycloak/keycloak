package org.freedesktop.dbus.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.messages.constants.ArgumentType;

/**
 * Builder to create Variants for parameterized types like Collections or Maps more easily.
 * <p>
 * When working with Variants you can use the default constructor which is taking your
 * object, but that will not work if you use Maps/Collections because the actual type(s) used inside
 * of those objects are not known on runtime (due to Java type erasure).
 * </p><p>
 * In this case you can use this builder providing the Class type you want to have inside of your
 * Variant (e.g. Map.class) and the generic class types used inside of the Map.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 *   VariantBuilder.of(Map.class).withGenericTypes(String.class, Integer.class).create(myMap);
 * </pre>
 * </p>
 *
 * @since 5.1.1 - 2024-08-16
 * @author hypfvieh
 */
public final class VariantBuilder {
    private final Class<?> baseClass;

    private final List<Class<?>> genericTypes = new ArrayList<>();

    private VariantBuilder(Class<?> _baseClass) {
        baseClass = _baseClass;
    }

    /**
     * Create a new instance using the given class as starting point.
     * <p>
     * If you want to create Variant containing a Map or List, this would be Map/List.class.
     * </p>
     * @param _clz class to use, never null
     * @return new instance
     */
    public static VariantBuilder of(Class<?> _clz) {
        return new VariantBuilder(Objects.requireNonNull(_clz, "Class required"));
    }

    /**
     * Add one or more generic types.
     * <p>
     * Use this if you want to create a Variant containing a Map, Collection.
     * You have to provide the data types used inside of your Map/Collection to this method.
     * E.g. you have Map&gt;Integer,String&lt; than you have to provide Integer.class and String.class to this method.
     * </p>
     *
     * @param _clz generic classes to add
     * @return this
     */
    public VariantBuilder withGenericTypes(Class<?>... _clz) {
        if (_clz == null || _clz.length == 0) {
            return this;
        }
        genericTypes.addAll(Arrays.asList(_clz));
        return this;
    }

    /**
     * Create the Variant instance using the provided data object.
     *
     * @param <X> Type inside of the Variant
     * @param _data data to store in Variant
     *
     * @return Variant
     *
     * @throws IllegalArgumentException when provided data object is not compatible with class given in constuctor
     * @throws NullPointerException when null is given
     */
    public <X> Variant<X> create(X _data) {
        Objects.requireNonNull(_data, "No data given");

        if (!baseClass.isAssignableFrom(_data.getClass())) {
            throw new IllegalArgumentException("Given data is not compatible with defined Variant base class");
        }

        StringBuilder sb = new StringBuilder();

        boolean isMap = false;
        if (Map.class.isAssignableFrom(baseClass)) {
            sb.append(ArgumentType.ARRAY_STRING).append(ArgumentType.DICT_ENTRY1_STRING);
            isMap = true;
        } else {
            sb.append(Marshalling.convertJavaClassesToSignature(baseClass));
        }

        genericTypes.stream()
            .map(Marshalling::convertJavaClassesToSignature)
            .forEach(sb::append);

        if (isMap) {
            sb.append(ArgumentType.DICT_ENTRY2_STRING);
        }

        return new Variant<>(_data, sb.toString());
    }

}
