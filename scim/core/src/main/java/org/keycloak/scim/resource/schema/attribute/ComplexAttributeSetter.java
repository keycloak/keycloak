package org.keycloak.scim.resource.schema.attribute;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.common.MultiValuedAttribute;

public class ComplexAttributeSetter<R extends ResourceTypeRepresentation> implements BiConsumer<R, String> {

    private final String name;
    private final String subName;
    private final Class<?> complexType;

    public ComplexAttributeSetter(String name, Class<?> complexType) {
        this(name, null, complexType);
    }

    public ComplexAttributeSetter(String name, String subName, Class<?> complexType) {
        Objects.requireNonNull(name);
        this.name = name;
        this.subName = subName;
        this.complexType = complexType;
    }

    @Override
    public void accept(R representation, String newValue) {
        try {
            Method declaredMethod = representation.getClass().getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
            Class<?> returnType = declaredMethod.getReturnType();
            Object value = declaredMethod.invoke(representation);
            Method setter = representation.getClass().getMethod("set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), returnType);

            if (value == null) {
                // no value yet, need to create it
                if (Collection.class.isAssignableFrom(returnType)) {
                    // if the return type is a collection, we need to create a new collection and add the new value to it
                    Collection<Object> values = new ArrayList<>();

                    setter.invoke(representation, values);

                    returnType = declaredMethod.getGenericReturnType() instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) declaredMethod.getGenericReturnType()).getActualTypeArguments()[0] : Object.class;

                    // for now lists can only be of complex types, so we need to check if the return type is assignable from the complex type
                    if (!complexType.isAssignableFrom(returnType)) {
                        throw new IllegalStateException("Return type of getter for attribute " + name + " must be a " + complexType.getName());
                    }

                    // if the complex type is a multivalued attribute, we need to create a new instance of the multi-valued attribute and add the new value to it
                    if (MultiValuedAttribute.class.isAssignableFrom(complexType)) {
                        Object item = returnType.getDeclaredConstructor().newInstance();
                        values.add(item);
                        item.getClass().getMethod("setValue", String.class).invoke(item, newValue);
                    }

                    // Currently only multivalued attributes are supported for complex attributes
                    return;
                } else if (complexType != null) {
                    // not multivalued, but still a complex type, so we need to create a new instance of the complex type and set it on the representation
                    value = complexType.getDeclaredConstructor().newInstance();
                    setter.invoke(representation, value);
                } else {
                    // if no complex type is defined, we assume operation in the representation itself, so we can just set the value on the representation
                    value = representation;
                }
            }

            if (String.class.isAssignableFrom(returnType) || Number.class.isAssignableFrom(returnType) || Boolean.class.isAssignableFrom(returnType)) {
                // simple value, just set it
                setter.invoke(representation, newValue);
            } else if (subName != null) {
                // nested attribute, we need to get the sub attribute and set the value on it
                if (subName.contains(".")) {
                    String[] parts = subName.split("\\.", 2);

                    if (parts.length > 2) {
                        throw new IllegalStateException("Can only handle one level of nesting for sub attributes, but got " + subName);
                    }

                    String subName = parts[1];
                    Method getSubMethod = value.getClass().getMethod("get" + Character.toUpperCase(parts[0].charAt(0)) + parts[0].substring(1));
                    Object subValue = getSubMethod.invoke(value);

                    if (subValue == null) {
                        subValue = getSubMethod.getReturnType().getDeclaredConstructor().newInstance();
                        Method setSubMethod = value.getClass().getMethod("set" + Character.toUpperCase(parts[0].charAt(0)) + parts[0].substring(1), subValue.getClass());
                        setSubMethod.invoke(value, subValue);
                    }

                    subValue.getClass().getMethod("set" + Character.toUpperCase(subName.charAt(0)) + subName.substring(1), String.class).invoke(subValue, newValue);
                } else {
                    value.getClass().getMethod("set" + Character.toUpperCase(subName.charAt(0)) + subName.substring(1), String.class).invoke(value, newValue);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set attribute " + name + " on representation " + representation.getClass().getName(), e);
        }
    }
}
