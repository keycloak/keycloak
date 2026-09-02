package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ReflectionUtils {

    public static List<Field> listFields(Class<?> clazz) {
        List<Field> fields = new LinkedList<>(Arrays.asList(clazz.getDeclaredFields()));

        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null && !superclass.equals(Object.class)) {
            fields.addAll(Arrays.asList(superclass.getDeclaredFields()));
            superclass = superclass.getSuperclass();
        }

        return fields;
    }

    public static List<Method> listMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        List<Method> methods = new LinkedList<>();
        List<Class<?>> hierarchy = new LinkedList<>();

        Class<?> current = clazz;
        while (current != null && !current.equals(Object.class)) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }

        for (Class<?> c : hierarchy) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getAnnotation(annotationClass) == null) {
                    continue;
                }

                if (methods.stream().noneMatch(e -> e.getName().equals(m.getName()) && Arrays.equals(e.getParameterTypes(), m.getParameterTypes()))) {
                    methods.add(0, m);
                }
            }
        }

        return methods;
    }

    public static void setField(Field field, Object object, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getValueType(Supplier<?, ?> supplier) {
        return getActualTypeArgument(supplier, 0);
    }

    public static Class<?> getAnnotationType(Supplier<?, ?> supplier) {
        return getActualTypeArgument(supplier, 1);
    }

    private static Class<?> getActualTypeArgument(Supplier<?, ?> supplier, int argument) {
        try {
            ParameterizedType parameterizedType = (ParameterizedType) supplier.getClass().getMethod("getValue", InstanceContext.class).getGenericParameterTypes()[0];
            return (Class<?>) parameterizedType.getActualTypeArguments()[argument];
        } catch (Throwable e) {
            throw new RuntimeException("Failed to discover generic types for supplier " + supplier.getClass().getName() + "; supplier must implement getValue method directly", e);
        }

    }

}
