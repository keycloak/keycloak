package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SupplierHelpers {

    public static <T> T getInstance(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String clazzName) {
        try {
            Class<T> clazz = (Class<T>) SupplierHelpers.class.getClassLoader().loadClass(clazzName);
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getAnnotationField(Annotation annotation, String name, T defaultValue) {
        T value = getAnnotationField(annotation, name);
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationField(Annotation annotation, String name) {
        if (annotation != null) {
            for (Method m : annotation.annotationType().getMethods()) {
                if (m.getName().equals(name)) {
                    try {
                        return (T) m.invoke(annotation);
                    } catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null;
    }

    public static String createName(InstanceContext<?, ?> instanceContext) {
        return instanceContext.getRef() != null ? instanceContext.getRef() : "default";
    }

}
