package org.keycloak.testframework.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultAnnotationProxyTest {

    @Test
    public void testGetField() {
        MockAnnotation proxy = DefaultAnnotationProxy.proxy(MockAnnotation.class, "");
        Assertions.assertEquals(LifeCycle.CLASS, proxy.lifecycle());
        Assertions.assertEquals(LinkedList.class, proxy.config());
        Assertions.assertEquals("", proxy.ref());
        Assertions.assertEquals("else", proxy.something());
    }

    @Test
    public void testCustomRef() {
        MockAnnotation proxy = DefaultAnnotationProxy.proxy(MockAnnotation.class, "myref");
        Assertions.assertEquals("myref", proxy.ref());
    }

    @Test
    public void testAnnotationReflection() {
        MockAnnotation proxy = DefaultAnnotationProxy.proxy(MockAnnotation.class, "");
        Assertions.assertEquals(LifeCycle.CLASS, SupplierHelpers.getAnnotationField(proxy, "lifecycle"));
        Assertions.assertEquals(LinkedList.class, SupplierHelpers.getAnnotationField(proxy, "config"));
        Assertions.assertEquals("", SupplierHelpers.getAnnotationField(proxy, "ref"));
        Assertions.assertEquals("else", SupplierHelpers.getAnnotationField(proxy, "something"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface MockAnnotation {

        Class<? extends List> config() default LinkedList.class;

        LifeCycle lifecycle() default LifeCycle.CLASS;

        String ref() default "";

        String something() default "else";

    }

}
