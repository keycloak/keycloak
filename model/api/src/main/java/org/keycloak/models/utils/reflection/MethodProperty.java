package org.keycloak.models.utils.reflection;

import java.lang.reflect.Method;

public interface MethodProperty<V> extends Property<V> {

    Method getAnnotatedElement();
}