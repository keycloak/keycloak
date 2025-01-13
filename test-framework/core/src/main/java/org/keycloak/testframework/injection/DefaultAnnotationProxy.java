package org.keycloak.testframework.injection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DefaultAnnotationProxy implements InvocationHandler {

    private final Class<?> annotationClass;
    private final String ref;

    public <S> DefaultAnnotationProxy(Class<?> annotationClass, String ref) {
        this.annotationClass = annotationClass;
        this.ref = ref;
    }

    public static <S> S proxy(Class<S> annotationClass, String ref) {
        return (S) Proxy.newProxyInstance(DefaultAnnotationProxy.class.getClassLoader(), new Class<?>[]{annotationClass}, new DefaultAnnotationProxy(annotationClass, ref));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("annotationType")) {
            return annotationClass;
        } else if (method.getName().equals("ref")) {
            return ref;
        } else {
            return annotationClass.getMethod(method.getName()).getDefaultValue();
        }
    }

}
