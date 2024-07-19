package org.keycloak.test.framework.injection;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.injection.mocks.MockChildAnnotation;
import org.keycloak.test.framework.injection.mocks.MockChildSupplier;
import org.keycloak.test.framework.injection.mocks.MockChildValue;
import org.keycloak.test.framework.injection.mocks.MockInstances;
import org.keycloak.test.framework.injection.mocks.MockParentAnnotation;
import org.keycloak.test.framework.injection.mocks.MockParentSupplier;
import org.keycloak.test.framework.injection.mocks.MockParentValue;

public class RegistryTest {

    private Registry registry;

    @BeforeEach
    public void resetMocks() {
        MockInstances.reset();
        MockParentSupplier.reset();
        MockChildSupplier.reset();
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
    public void testDependencyCreatedOnDemand() {
        ChildTest childTest = new ChildTest();

        registry.beforeEach(childTest);
        assertRunning(childTest.child, childTest.child.getParent());
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

}
