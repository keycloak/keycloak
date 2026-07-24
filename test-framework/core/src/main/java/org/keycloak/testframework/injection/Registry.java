package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.keycloak.testframework.FatalTestClassException;
import org.keycloak.testframework.TestFrameworkExecutor;
import org.keycloak.testframework.annotations.TestCleanup;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.injection.predicates.DependencyPredicates;
import org.keycloak.testframework.injection.predicates.InstanceContextPredicates;
import org.keycloak.testframework.injection.predicates.RequestedInstancePredicates;
import org.keycloak.testframework.injection.predicates.TestFrameworkExecutorPredicates;
import org.keycloak.testframework.server.KeycloakServer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Registry implements AutoCloseable {

    private final RegistryLogger logger;

    private ExtensionContext currentContext;
    private final Extensions extensions;
    private final List<InstanceContext<?, ?>> deployedInstances = new LinkedList<>();
    private final List<RequestedInstance<?, ?>> requestedInstances = new LinkedList<>();
    private FatalTestClassException fatalTestClassException;

    private Object currentTestInstance;

    public Registry() {
        extensions = Extensions.getInstance();
        logger = new RegistryLogger(extensions.getValueTypeAlias());
    }

    RegistryLogger getLogger() {
        return logger;
    }

    Extensions getExtensions() {
        return extensions;
    }

    public ExtensionContext getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(ExtensionContext currentContext) {
        this.currentContext = currentContext;
    }

    public <T> T getDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        ref = StringUtil.convertEmptyToNull(ref);

        List<Dependency> declaredDependencies = dependent.getDeclaredDependencies();
        if (declaredDependencies.stream().noneMatch(DependencyPredicates.matches(typeClass, ref))) {
            throw new RuntimeException("Tried to retrieve non-declared dependency " + typeClass.getSimpleName() + ":" + ref);
        }

        T dependency;
        dependency = getDeployedDependency(typeClass, ref, dependent);
        if (dependency != null) {
            return dependency;
        } else {
            dependency = getRequestedDependency(typeClass, ref, dependent);
            if (dependency != null) {
                return dependency;
            }
        }

        throw new RuntimeException("Dependency not found: " + typeClass);
    }

    public List<InstanceContext<?, ?>> getDeployedInstances() {
        return deployedInstances;
    }

    public List<RequestedInstance<?, ?>> getRequestedInstances() {
        return requestedInstances;
    }

    private <T> T getDeployedDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        InstanceContext dependency = getDeployedInstance(typeClass, ref);
        if (dependency != null) {
            dependency.registerDependent(dependent);

            logger.logDependencyInjection(dependent, dependency, RegistryLogger.InjectionType.EXISTING);

            return (T) dependency.getValue();
        }
        return null;
    }

    private <T> T getRequestedDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        RequestedInstance requestedDependency = getRequestedInstance(typeClass, ref);
        if (requestedDependency != null) {
            InstanceContext dependency = new InstanceContext<Object, Annotation>(requestedDependency.getInstanceId(), this, requestedDependency.getSupplier(), requestedDependency.getAnnotation(), requestedDependency.getValueType(), requestedDependency.getDeclaredDependencies());
            dependency.setValue(requestedDependency.getSupplier().getValue(dependency));
            dependency.registerDependent(dependent);
            deployedInstances.add(dependency);

            requestedInstances.remove(requestedDependency);

            logger.logDependencyInjection(dependent, dependency, RegistryLogger.InjectionType.REQUESTED);

            return (T) dependency.getValue();
        }
        return null;
    }

    public void beforeEach(Object testInstance, Method testMethod) {
        if (fatalTestClassException != null) {
            skipTestMethod();
        }

        try {
            findRequestedInstances(testInstance, testMethod);
            destroyIncompatibleInstances();
            matchDeployedInstancesWithRequestedInstances();
            deployRequestedInstances();
            invokeBeforeEachOnSuppliers();
            injectFields(testInstance);

            if (currentTestInstance == null || testInstance.getClass() != currentTestInstance.getClass()) {
                executeSetup(testInstance, TestSetup.class);
                currentTestInstance = testInstance;
            }

        } catch (FatalTestClassException e) {
            requestedInstances.clear();
            fatalTestClassException = e;
            skipTestMethod();
        }
    }

    private void skipTestMethod() {
        Assumptions.abort("Skipping test method due to fatal test class error");
    }

    public void intercept(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext) throws Throwable {
        Class<?> testClass = invocationContext.getTargetClass();
        Method testMethod = invocationContext.getExecutable();

        TestFrameworkExecutor testFrameworkExecutor = getExecutor(testMethod);
        if (testFrameworkExecutor != null) {
            testFrameworkExecutor.execute(this, testClass, testMethod);
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Method testMethod = (Method) parameterContext.getParameter().getDeclaringExecutable();
        Class<?> parameterType = parameterContext.getParameter().getType();
        TestFrameworkExecutor testFrameworkExecutor = getExecutor(testMethod);
        return testFrameworkExecutor != null && testFrameworkExecutor.supportsParameter(testMethod, parameterType);
    }

    private void findRequestedInstances(Object testInstance, Method testMethod) {
        List<Class<?>> alwaysEnabledValueTypes = extensions.getAlwaysEnabledValueTypes();
        for (Class<?> valueType : alwaysEnabledValueTypes) {
            RequestedInstance requestedInstance = createRequestedInstance(null, valueType);
            if (requestedInstance != null) {
                requestedInstances.add(requestedInstance);
            }
        }

        List<Class<?>> methodValueTypes = extensions.getMethodValueTypes(testMethod);
        for (Class<?> valueType : methodValueTypes) {
            RequestedInstance requestedInstance = createRequestedInstance(null, valueType);
            if (requestedInstance != null) {
                requestedInstances.add(requestedInstance);
            }
        }

        Class testClass = testInstance.getClass();
        RequestedInstance requestedServerInstance = createRequestedInstance(testClass.getAnnotations(), KeycloakServer.class);
        if (requestedServerInstance != null) {
            requestedInstances.add(requestedServerInstance);
        }

        for (Field f : ReflectionUtils.listFields(testClass)) {
            RequestedInstance requestedInstance = createRequestedInstance(f.getAnnotations(), f.getType());
            if (requestedInstance != null) {
                requestedInstances.add(requestedInstance);
            }
        }

        DependencyGraphResolver dependencyGraphResolver = new DependencyGraphResolver(this);
        List<RequestedInstance<?, ?>> missingInstances = dependencyGraphResolver.getMissingInstances();
        requestedInstances.addAll(missingInstances);

        logger.logRequestedInstances(requestedInstances);
    }

    private void destroyIncompatibleInstances() {
        for (RequestedInstance<?, ?> requestedInstance : requestedInstances) {
            InstanceContext deployedInstance = getDeployedInstance(requestedInstance);
            if (deployedInstance != null) {
                boolean compatible = requestedInstance.getLifeCycle().equals(deployedInstance.getLifeCycle()) && deployedInstance.getSupplier().compatible(deployedInstance, requestedInstance);
                if (!compatible) {
                    logger.logDestroyIncompatible(deployedInstance);
                    destroy(deployedInstance);
                }
            }
        }
    }

    private void matchDeployedInstancesWithRequestedInstances() {
        Iterator<RequestedInstance<?, ?>> itr = requestedInstances.iterator();
        while (itr.hasNext()) {
            RequestedInstance<?, ?> requestedInstance = itr.next();
            InstanceContext deployedInstance = getDeployedInstance(requestedInstance);
            if (deployedInstance != null) {
                if (requestedInstance.getLifeCycle().equals(deployedInstance.getLifeCycle()) && deployedInstance.getSupplier().compatible(deployedInstance, requestedInstance)) {
                    logger.logReusingCompatibleInstance(deployedInstance);
                    itr.remove();
                }
            }
        }
    }

    private void deployRequestedInstances() {
        requestedInstances.sort(RequestedInstanceComparator.INSTANCE);

        while (!requestedInstances.isEmpty()) {
            RequestedInstance nextToDeploy = requestedInstances.stream().filter(r -> {
                List<Dependency> declaredDependencies = r.getDeclaredDependencies();
                for (Dependency d : declaredDependencies) {
                    if (deployedInstances.stream().noneMatch(InstanceContextPredicates.matches(d.valueType(), d.ref()))) {
                        return false;
                    }
                }
                return true;
            }).findFirst().orElseThrow(() -> new RuntimeException("Failed to resolve next requested instance to deploy"));

            requestedInstances.remove(nextToDeploy);

            if (getDeployedInstance(nextToDeploy) == null) {
                InstanceContext instance = new InstanceContext(nextToDeploy.getInstanceId(), this, nextToDeploy.getSupplier(), nextToDeploy.getAnnotation(), nextToDeploy.getValueType(), nextToDeploy.getDeclaredDependencies());
                instance.setValue(nextToDeploy.getSupplier().getValue(instance));
                deployedInstances.add(instance);

                if (!nextToDeploy.getDependents().isEmpty()) {
                    Set<InstanceContext<?,?>> dependencies = nextToDeploy.getDependents();
                    dependencies.forEach(instance::registerDependent);
                }

                logger.logCreatedInstance(nextToDeploy, instance);
            }
        }
    }

    private void injectFields(Object testInstance) {
        for (Field f : ReflectionUtils.listFields(testInstance.getClass())) {
            InstanceContext<?, ?> instance = getDeployedInstance(f.getType(), f.getAnnotations());
            if (instance == null) { // a test class might have fields not meant for injection
                continue;
            }
            ReflectionUtils.setField(f, testInstance, instance.getValue());
        }
    }

    private void executeSetup(Object testInstance, Class<? extends Annotation> annotation) {
        for (Method m : ReflectionUtils.listMethods(testInstance.getClass(), annotation)) {
            if (m.getParameterCount() != 0) {
                throw new RuntimeException("Method with " + annotation.getName() + " has required parameters: " + m); // Update when https://github.com/keycloak/keycloak/pull/45869 is merged
            }
            try {
                m.invoke(testInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Method with " + annotation.getName() + " not accessible: " + m); // Update when https://github.com/keycloak/keycloak/pull/45869 is merged
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void afterAll() {
        FatalTestClassException exception = fatalTestClassException;
        fatalTestClassException = null;

        if (exception == null && currentTestInstance != null) {
            executeSetup(currentTestInstance, TestCleanup.class);
        }

        logger.logAfterAll();
        List<InstanceContext<?, ?>> destroy = deployedInstances.stream().filter(InstanceContextPredicates.hasLifeCycle(LifeCycle.CLASS)).toList();
        destroy.forEach(this::destroy);

        if (exception != null) {
            throw exception;
        }
    }

    public void afterEach() {
        logger.logAfterEach();
        List<InstanceContext<?, ?>> destroy = deployedInstances.stream().filter(InstanceContextPredicates.hasLifeCycle(LifeCycle.METHOD)).toList();
        destroy.forEach(this::destroy);

        List<InstanceContext<?, ?>> cleanup = deployedInstances.stream().filter(InstanceContextPredicates.isInstanceof(ManagedTestResource.class)).toList();
        for (InstanceContext<?, ?> c : cleanup) {
            ManagedTestResource managedTestResource = (ManagedTestResource) c.getValue();
            if (managedTestResource.isDirty()) {
                logger.logDestroyDirty(c);
                destroy(c);
            } else {
                logger.logCleanup(c);
                managedTestResource.runCleanup();
            }
        }
    }

    public void close() {
        logger.logClose();
        List<InstanceContext<?, ?>> destroy = deployedInstances.stream().sorted(InstanceContextComparator.INSTANCE.reversed()).toList();
        destroy.forEach(this::destroy);
    }

    List<Supplier<?, ?>> getSuppliers() {
        return extensions.getSuppliers();
    }

    RequestedInstance<?, ?> createRequestedInstance(Annotation[] annotations, Class<?> valueType) {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                Supplier<?, ?> supplier = extensions.findSupplierByAnnotation(annotation);
                if (supplier != null) {
                    if (!supplier.getValueType().isAssignableFrom(valueType)) {
                        throw typeMismatch(annotation.annotationType(), supplier.getValueType(), valueType);
                    }
                    return new RequestedInstance(supplier, annotation, valueType);
                }
            }
        } else {
            Supplier<?, ?> supplier = extensions.findSupplierByType(valueType);
            if (supplier != null) {
                Annotation defaultAnnotation = DefaultAnnotationProxy.proxy(supplier.getAnnotationClass(), null);
                return new RequestedInstance(supplier, defaultAnnotation, valueType);
            }
        }
        return null;
    }

    private InstanceContext<?, ?> getDeployedInstance(Class<?> valueType, Annotation[] annotations) {
        for (Annotation a : annotations) {
            for (InstanceContext<?, ?> i : deployedInstances) {
                Supplier supplier = i.getSupplier();
                if (supplier.getAnnotationClass().equals(a.annotationType())
                        && valueType.isAssignableFrom(i.getValue().getClass())
                        && Objects.equals(supplier.getRef(a), i.getRef())) {
                    return i;
                }
            }
        }
        return null;
    }

    private void destroy(InstanceContext instanceContext) {
        boolean removed = deployedInstances.remove(instanceContext);
        if (removed) {
            Set<InstanceContext> dependencies = instanceContext.getDependents();
            dependencies.forEach(this::destroy);
            instanceContext.getSupplier().close(instanceContext);

            logger.logDestroy(instanceContext);
        }
    }

    private InstanceContext getDeployedInstance(RequestedInstance requestedInstance) {
        String requestedRef = requestedInstance.getRef();
        Class requestedValueType = requestedInstance.getValueType();
        for (InstanceContext<?, ?> i : deployedInstances) {
            if (!Objects.equals(i.getRef(), requestedRef)) {
                continue;
            }

            if (requestedValueType != null) {
                if (requestedValueType.isAssignableFrom(i.getValue().getClass())) {
                    return i;
                }
            } else if (i.getSupplier().equals(requestedInstance.getSupplier())) {
                return i;
            }
        }
        return null;
    }

    private InstanceContext getDeployedInstance(Class typeClass, String ref) {
        return deployedInstances.stream()
                .filter(InstanceContextPredicates.matches(typeClass, ref))
                .findFirst().orElse(null);
    }

    private RequestedInstance getRequestedInstance(Class typeClass, String ref) {
        return requestedInstances.stream()
                .filter(RequestedInstancePredicates.matches(typeClass, ref))
                .findFirst().orElse(null);
    }

    private void invokeBeforeEachOnSuppliers() {
        for (InstanceContext i : deployedInstances) {
            i.getSupplier().onBeforeEach(i);
        }
    }

    private TestFrameworkExecutor getExecutor(Method testMethod) {
        return extensions.getTestFrameworkExecutors().stream().filter(TestFrameworkExecutorPredicates.shouldExecute(testMethod)).findFirst().orElse(null);
    }

    private FatalTestClassException typeMismatch(
            Class<? extends Annotation> annotation,
            Class<?> expectedType,
            Class<?> providedType) {
        return new FatalTestClassException(
                String.format("@%s requires %s (or its subclass) but field has type %s",
                        annotation.getSimpleName(),
                        expectedType.getName(),
                        providedType.getName())
        );
    }

    private static class RequestedInstanceComparator implements Comparator<RequestedInstance> {

        static final RequestedInstanceComparator INSTANCE = new RequestedInstanceComparator();

        @Override
        public int compare(RequestedInstance o1, RequestedInstance o2) {
            return Integer.compare(o1.getSupplier().order(), o2.getSupplier().order());
        }
    }

    private static class InstanceContextComparator implements Comparator<InstanceContext> {

        static final InstanceContextComparator INSTANCE = new InstanceContextComparator();

        @Override
        public int compare(InstanceContext o1, InstanceContext o2) {
            return Integer.compare(o1.getSupplier().order(), o2.getSupplier().order());
        }
    }

}
