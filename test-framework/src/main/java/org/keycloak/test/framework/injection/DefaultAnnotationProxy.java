package org.keycloak.test.framework.injection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DefaultAnnotationProxy implements InvocationHandler {

    private final Class<?> annotationClass;

    public <S> DefaultAnnotationProxy(Class<?> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public static <S> S proxy(Class<S> annotationClass) {
        return (S) Proxy.newProxyInstance(DefaultAnnotationProxy.class.getClassLoader(), new Class<?>[]{annotationClass}, new DefaultAnnotationProxy(annotationClass));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("annotationType")) {
            return annotationClass;
        } else {
            return annotationClass.getMethod(method.getName()).getDefaultValue();
        }
    }

}
