package org.keycloak.testframework.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReflectionUtilsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface MockSetup {
    }

    static class SuperClass {
        @MockSetup
        void superSetup() {
        }
    }

    static class SubClass extends SuperClass {
        @MockSetup
        void subSetup() {
        }
    }

    static class BaseWithSetup {
        @MockSetup
        void setup() {
        }
    }

    static class OverridingChild extends BaseWithSetup {
        @MockSetup
        @Override
        void setup() {
        }
    }

    static class GrandParent {
        @MockSetup
        void grandParentSetup() {
        }
    }

    static class Parent extends GrandParent {
        @MockSetup
        void parentSetup() {
        }
    }

    static class Child extends Parent {
        @MockSetup
        void childSetup() {
        }
    }

    static class NoAnnotatedMethods {
        void notAnnotated() {
        }
    }

    static class PlainBase {
    }

    static class AnnotatedChild extends PlainBase {
        @MockSetup
        void childOnly() {
        }
    }

    @Test
    public void superclassMethodRunsBeforeSubclass() {
        List<Method> methods = ReflectionUtils.listMethods(SubClass.class, MockSetup.class);
        List<String> names = methods.stream().map(Method::getName).collect(Collectors.toList());

        Assertions.assertEquals(List.of("superSetup", "subSetup"), names);
    }

    @Test
    public void overriddenMethodOnlyRunsOnce() {
        List<Method> methods = ReflectionUtils.listMethods(OverridingChild.class, MockSetup.class);
        List<String> names = methods.stream().map(Method::getName).collect(Collectors.toList());

        Assertions.assertEquals(1, methods.size());
        Assertions.assertEquals("setup", names.get(0));
        Assertions.assertEquals(OverridingChild.class, methods.get(0).getDeclaringClass());
    }

    @Test
    public void multiLevelInheritanceOrdering() {
        List<Method> methods = ReflectionUtils.listMethods(Child.class, MockSetup.class);
        List<String> names = methods.stream().map(Method::getName).collect(Collectors.toList());

        Assertions.assertEquals(List.of("grandParentSetup", "parentSetup", "childSetup"), names);
    }

    @Test
    public void noAnnotatedMethodsReturnsEmpty() {
        List<Method> methods = ReflectionUtils.listMethods(NoAnnotatedMethods.class, MockSetup.class);

        Assertions.assertTrue(methods.isEmpty());
    }

    @Test
    public void singleClassWithNoInheritance() {
        List<Method> methods = ReflectionUtils.listMethods(SuperClass.class, MockSetup.class);

        Assertions.assertEquals(1, methods.size());
        Assertions.assertEquals("superSetup", methods.get(0).getName());
    }

    @Test
    public void childWithPlainBaseClass() {
        List<Method> methods = ReflectionUtils.listMethods(AnnotatedChild.class, MockSetup.class);

        Assertions.assertEquals(1, methods.size());
        Assertions.assertEquals("childOnly", methods.get(0).getName());
    }
}
