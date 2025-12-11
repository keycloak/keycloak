package org.keycloak.testframework.injection;

import java.util.List;

import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.mocks.MockChildAnnotation;
import org.keycloak.testframework.injection.mocks.MockChildSupplier;
import org.keycloak.testframework.injection.mocks.MockChildValue;
import org.keycloak.testframework.injection.mocks.MockInstances;
import org.keycloak.testframework.injection.mocks.MockParent2Supplier;
import org.keycloak.testframework.injection.mocks.MockParentAnnotation;
import org.keycloak.testframework.injection.mocks.MockParentSupplier;
import org.keycloak.testframework.injection.mocks.MockParentValue;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RegistryTest {

    private Registry registry;

    @BeforeEach
    public void resetMocks() {
        MockInstances.reset();
        MockParentSupplier.reset();
        MockChildSupplier.reset();
        Extensions.reset();
        registry = new Registry();
    }

    @Test
    public void testGlobalLifeCycle() {
        MockParentSupplier.DEFAULT_LIFECYCLE = LifeCycle.GLOBAL;
        ParentTest test = new ParentTest();

        registry.beforeEach(test);
        MockParentValue value1 = test.parent;
        assertRunning(value1);

        registry.afterEach();
        registry.afterAll();
        assertRunning(value1);

        ParentTest test2 = new ParentTest();
        registry.beforeEach(test2);
        MockParentValue value2 = test2.parent;

        Assertions.assertSame(value1, value2);

        registry.close();

        assertClosed(value1);
    }

    @Test
    public void testClassLifeCycle() {
        ParentTest test = new ParentTest();

        registry.beforeEach(test);
        MockParentValue value1 = test.parent;
        assertRunning(value1);

        registry.afterEach();
        assertRunning(value1);

        registry.beforeEach(test);
        MockParentValue value2 = test.parent;
        Assertions.assertSame(value1, value2);

        registry.afterEach();
        assertRunning(value1);

        registry.afterAll();
        assertClosed(value1);

        ParentTest test2 = new ParentTest();
        registry.beforeEach(test2);
        MockParentValue value3 = test2.parent;

        Assertions.assertNotSame(value1, value3);
    }

    @Test
    public void testMethodLifeCycle() {
        MockParentSupplier.DEFAULT_LIFECYCLE = LifeCycle.METHOD;

        ParentTest test = new ParentTest();

        registry.beforeEach(test);
        MockParentValue value1 = test.parent;
        assertRunning(value1);

        registry.afterEach();
        assertClosed(value1);

        registry.beforeEach(test);
        MockParentValue value2 = test.parent;
        Assertions.assertNotSame(value1, value2);
    }

    @Test
    public void testLifeCycleBetweenDependencies() {
        MockParentSupplier.DEFAULT_LIFECYCLE = LifeCycle.METHOD;
        MockChildSupplier.DEFAULT_LIFECYCLE = LifeCycle.GLOBAL;

        ParentAndChildTest parentAndChildTest = new ParentAndChildTest();
        MockChildValue child1 = parentAndChildTest.child;

        registry.beforeEach(parentAndChildTest);
        assertRunning(parentAndChildTest.parent, parentAndChildTest.child);
        Assertions.assertSame(parentAndChildTest.parent, parentAndChildTest.child.getParent());

        registry.afterEach();
        assertClosed(parentAndChildTest.parent, parentAndChildTest.child);

        registry.beforeEach(parentAndChildTest);
        Assertions.assertNotSame(child1, parentAndChildTest.child);
    }

    @Test
    public void testRecreateIfDifferentLifeCycleRequested() {
        MockParentSupplier.DEFAULT_LIFECYCLE = LifeCycle.GLOBAL;

        ParentTest parentTest = new ParentTest();

        registry.beforeEach(parentTest);
        registry.afterEach();
        registry.afterAll();
        assertRunning(parentTest.parent);

        MockParentSupplier.DEFAULT_LIFECYCLE = LifeCycle.CLASS;

        ParentTest parentTest2 = new ParentTest();
        registry.beforeEach(parentTest2);

        assertRunning(parentTest2.parent);
        assertClosed(parentTest.parent);
        Assertions.assertNotSame(parentTest2.parent, parentTest.parent);
    }

    @Test
    public void testRecreateIfNotCompatible() {
        ParentTest parentTest = new ParentTest();

        registry.beforeEach(parentTest);
        MockParentValue parent1 = parentTest.parent;
        registry.afterEach();

        MockParentSupplier.COMPATIBLE = false;

        registry.beforeEach(parentTest);
        registry.afterEach();

        MockParentValue parent2 = parentTest.parent;

        assertRunning(parent2);
        assertClosed(parent1);
        Assertions.assertNotSame(parent2, parent1);
    }

    @Test
    public void testSelectedSupplierDefault() {
        List<Supplier<?, ?>> suppliers = registry.getSuppliers();
        Assertions.assertEquals(1, suppliers.stream().filter(s -> s.getValueType().equals(MockParentValue.class)).count());
        Assertions.assertTrue(suppliers.stream().anyMatch(s -> s.getClass().equals(MockParentSupplier.class)));
        Assertions.assertFalse(suppliers.stream().anyMatch(s -> s.getClass().equals(MockParent2Supplier.class)));
    }

    @Test
    public void testSelectedSupplierConfigOverride() {
        System.setProperty("kc.test.MockParentValue", MockParent2Supplier.class.getSimpleName());
        try {
            Config.initConfig();

            Extensions.reset();
            registry = new Registry();
            List<Supplier<?, ?>> suppliers = registry.getSuppliers();
            Assertions.assertEquals(1, suppliers.stream().filter(s -> s.getValueType().equals(MockParentValue.class)).count());
            Assertions.assertFalse(suppliers.stream().anyMatch(s -> s.getClass().equals(MockParentSupplier.class)));
            Assertions.assertTrue(suppliers.stream().anyMatch(s -> s.getClass().equals(MockParent2Supplier.class)));
        } finally {
            System.getProperties().remove("kc.test.MockParentValue");
            Config.initConfig();
        }
    }

    @Test
    public void testDependencyCreatedOnDemand() {
        ChildTest childTest = new ChildTest();

        registry.beforeEach(childTest);
        assertRunning(childTest.child, childTest.child.getParent());
    }

    @Test
    public void testDependencyRequestedBefore() {
        ParentAndChildTest test = new ParentAndChildTest();

        registry.beforeEach(test);
        assertRunning(test.child, test.child.getParent());
    }

    @Test
    public void testDependencyRequestedAfter() {
        ChildAndParentTest test = new ChildAndParentTest();

        registry.beforeEach(test);
        assertRunning(test.child, test.child.getParent());
    }

    @Test
    public void testMultiplRef() {
        MultipleRefTest refTest = new MultipleRefTest();
        registry.beforeEach(refTest);

        MockParentValue def1 = refTest.def;
        MockParentValue a1 = refTest.a;

        Assertions.assertNotSame(refTest.def, refTest.a);
        Assertions.assertNotSame(refTest.def, refTest.b);
        Assertions.assertNotSame(refTest.a, refTest.b);

        assertRunning(refTest.def, refTest.a, refTest.b);
        Assertions.assertSame(refTest.a, refTest.a2);

        registry.afterEach();

        registry.beforeEach(refTest);
        assertRunning(refTest.def, refTest.a2, refTest.b);

        Assertions.assertSame(def1, refTest.def);
        Assertions.assertSame(a1, refTest.a);

        registry.afterEach();
        assertRunning(refTest.def, refTest.a, refTest.b);

        registry.afterAll();
        assertClosed(refTest.def, refTest.a, refTest.b);
    }

    @Test
    public void testRealmRef() {
        RealmRefTest test = new RealmRefTest();
        registry.beforeEach(test);

        assertRunning(test.childABC, test.childABC.getParent(), test.child123, test.parent123);
        Assertions.assertNotSame(test.childABC.getParent(), test.parent123);
        Assertions.assertSame(test.child123.getParent(), test.parent123);
    }

    @Test
    public void testConfigurableSupplier() {
        ParentTest parentTest = new ParentTest();
        registry.beforeEach(parentTest);

        Assertions.assertNull(parentTest.parent.getStringOption());
        Assertions.assertTrue(parentTest.parent.isBooleanOption());

        System.setProperty("kc.test.MockParentValue.string", "some string");
        System.setProperty("kc.test.MockParentValue.boolean", "false");

        try {
            Config.initConfig();
            Extensions.reset();
            registry = new Registry();
            parentTest = new ParentTest();
            registry.beforeEach(parentTest);

            Assertions.assertEquals("some string", parentTest.parent.getStringOption());
            Assertions.assertFalse(parentTest.parent.isBooleanOption());
        } finally {
            System.getProperties().remove("kc.test.MockParentValue.string");
            System.getProperties().remove("kc.test.MockParentValue.boolean");
            Config.initConfig();
        }
    }

    @Test
    public void testIncompatibleParent() {
        MockParentSupplier.COMPATIBLE = false;
        RealmIncompatibleParentTest test = new RealmIncompatibleParentTest();
        registry.beforeEach(test);

        MockParentValue parent1 = test.parent;
        MockChildValue child1 = test.child;

        Assertions.assertNotNull(test.parent);
        Assertions.assertNotNull(test.child);

        registry.afterEach();

        registry.beforeEach(test);

        Assertions.assertNotNull(test.parent);
        Assertions.assertNotEquals(parent1, test.parent);
        Assertions.assertNotNull(test.child);
        Assertions.assertNotEquals(child1, test.child);
    }

    public static void assertRunning(Object... values) {
        MatcherAssert.assertThat(MockInstances.INSTANCES, Matchers.hasItems(values));
        MatcherAssert.assertThat(MockInstances.INSTANCES, Matchers.hasSize(values.length));
    }

    public static void assertClosed(Object... values) {
        MatcherAssert.assertThat(MockInstances.CLOSED_INSTANCES, Matchers.hasItems(values));
        MatcherAssert.assertThat(MockInstances.CLOSED_INSTANCES, Matchers.hasSize(values.length));
    }

    public static final class ParentAndChildTest {
        @MockParentAnnotation
        MockParentValue parent;

        @MockChildAnnotation
        MockChildValue child;
    }

    public static final class ParentTest {
        @MockParentAnnotation
        MockParentValue parent;
    }

    public static final class ChildTest {
        @MockChildAnnotation
        MockChildValue child;
    }

    public static final class ChildAndParentTest {
        @MockChildAnnotation
        MockChildValue child;

        @MockParentAnnotation
        MockParentValue parent;
    }

    public static final class MultipleRefTest {
        @MockParentAnnotation()
        MockParentValue def;

        @MockParentAnnotation(ref = "a")
        MockParentValue a;

        @MockParentAnnotation(ref = "a")
        MockParentValue a2;

        @MockParentAnnotation(ref = "b")
        MockParentValue b;
    }

    public static final class RealmRefTest {
        @MockParentAnnotation(ref = "123")
        MockParentValue parent123;

        @MockChildAnnotation(ref = "123", parentRef = "123")
        MockChildValue child123;

        @MockChildAnnotation(ref = "ABC", parentRef = "ABC")
        MockChildValue childABC;
    }

    public static final class RealmIncompatibleParentTest {

        @MockChildAnnotation
        MockChildValue child;

        @MockParentAnnotation
        MockParentValue parent;

    }

}
