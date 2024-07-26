package org.keycloak.test.framework.injection;

import java.lang.reflect.Constructor;

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

}
