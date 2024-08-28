package org.keycloak.test.framework.injection;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.test.framework.config.Config;
import org.keycloak.test.framework.realm.DefaultRealmConfig;
import org.keycloak.test.framework.realm.ManagedRealm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Registry implements ExtensionContext.Store.CloseableResource {

    private static final Logger LOGGER = Logger.getLogger(Registry.class);

    private ExtensionContext currentContext;
    private final List<Supplier<?, ?>> suppliers = new LinkedList<>();
    private final List<InstanceContext<?, ?>> deployedInstances = new LinkedList<>();
    private final List<RequestedInstance<?, ?>> requestedInstances = new LinkedList<>();

    public Registry() {
        loadSuppliers();
    }

    public ExtensionContext getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(ExtensionContext currentContext) {
        this.currentContext = currentContext;
    }

    public <T> T getDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        ref = StringUtil.convertEmptyToNull(ref);
        T dependency;
        dependency = getDeployedDependency(typeClass, ref, dependent);
        if (dependency != null) {
            return dependency;
        } else {
            dependency = getRequestedDependency(typeClass, ref, dependent);
            if(dependency != null) {
                return dependency;
            } else {
                dependency = getUnConfiguredDependency(typeClass, ref, dependent);
                if(dependency != null) {
                    return dependency;
                }
            }
        }

        throw new RuntimeException("Dependency not found: " + typeClass);
    }

    private <T> T getDeployedDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        InstanceContext dependency = getDeployedInstance(typeClass, ref);
        if (dependency != null) {
            dependency.registerDependency(dependent);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Injecting existing dependency {0} into {1}",
                        dependency.getSupplier().getClass().getSimpleName(),
                        dependent.getSupplier().getClass().getSimpleName());
            }

            return (T) dependency.getValue();
        }
        return null;
    }

    private <T> T getRequestedDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        RequestedInstance requestedDependency = getRequestedInstance(typeClass, ref);
        if (requestedDependency != null) {
            InstanceContext dependency = new InstanceContext<Object, Annotation>(this, requestedDependency.getSupplier(), requestedDependency.getAnnotation(), requestedDependency.getValueType());
            dependency.setValue(requestedDependency.getSupplier().getValue(dependency));
            dependency.registerDependency(dependent);
            deployedInstances.add(dependency);

            requestedInstances.remove(requestedDependency);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Injecting requested dependency {0} into {1}",
                        dependency.getSupplier().getClass().getSimpleName(),
                        dependent.getSupplier().getClass().getSimpleName());
            }

            return (T) dependency.getValue();
        }
        return null;
    }

    private <T> T getUnConfiguredDependency(Class<T> typeClass, String ref, InstanceContext dependent) {
        InstanceContext dependency;
        Optional<Supplier<?, ?>> supplied = suppliers.stream().filter(s -> s.getValueType().equals(typeClass)).findFirst();
        if (supplied.isPresent()) {
            Supplier<T, ?> supplier = (Supplier<T, ?>) supplied.get();
            Annotation defaultAnnotation = DefaultAnnotationProxy.proxy(supplier.getAnnotationClass());
            dependency = new InstanceContext(this, supplier, defaultAnnotation, typeClass);

            dependency.registerDependency(dependent);
            dependency.setValue(supplier.getValue(dependency));

            deployedInstances.add(dependency);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Injecting un-configured dependency {0} into {1}",
                        dependency.getSupplier().getClass().getSimpleName(),
                        dependent.getSupplier().getClass().getSimpleName());
            }

            return (T) dependency.getValue();
        }
        return null;
    }

    public void beforeEach(Object testInstance) {
        findRequestedInstances(testInstance);
        matchDeployedInstancesWithRequestedInstances();
        deployRequestedInstances();
        injectFields(testInstance);
        invokeBeforeEachOnSuppliers();
    }

    private void findRequestedInstances(Object testInstance) {
        Class testClass = testInstance.getClass();
        RequestedInstance requestedServerInstance = createRequestedInstance(testClass.getAnnotations(), null);
        if (requestedServerInstance != null) {
            requestedInstances.add(requestedServerInstance);
        }

        for (Field f : testClass.getDeclaredFields()) {
            RequestedInstance requestedInstance = createRequestedInstance(f.getAnnotations(), f.getType());
            if (requestedInstance != null) {
                requestedInstances.add(requestedInstance);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracev("Requested suppliers: {0}",
                    requestedInstances.stream().map(r -> r.getSupplier().getClass().getSimpleName()).collect(Collectors.joining(", ")));
        }
    }

    private void matchDeployedInstancesWithRequestedInstances() {
        Iterator<RequestedInstance<?, ?>> itr = requestedInstances.iterator();
        while (itr.hasNext()) {
            RequestedInstance<?, ?> requestedInstance = itr.next();
            InstanceContext deployedInstance = getDeployedInstance(requestedInstance);
            if (deployedInstance != null) {
                if (requestedInstance.getLifeCycle().equals(deployedInstance.getLifeCycle()) && deployedInstance.getSupplier().compatible(deployedInstance, requestedInstance)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.tracev("Reusing compatible: {0}",
                                deployedInstance.getSupplier().getClass().getSimpleName());
                    }

                    itr.remove();
                } else {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.tracev("Destroying non-compatible: {0}",
                                deployedInstance.getSupplier().getClass().getSimpleName());
                    }

                    destroy(deployedInstance);
                }
            }
        }
    }

    private void deployRequestedInstances() {
        while (!requestedInstances.isEmpty()) {
            RequestedInstance requestedInstance = requestedInstances.remove(0);

            if (getDeployedInstance(requestedInstance) == null) {
                InstanceContext instance = new InstanceContext(this, requestedInstance.getSupplier(), requestedInstance.getAnnotation(), requestedInstance.getValueType());
                instance.setValue(requestedInstance.getSupplier().getValue(instance));
                deployedInstances.add(instance);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.tracev("Created instance: {0}",
                            requestedInstance.getSupplier().getClass().getSimpleName());
                }
            }
        }
    }

    private void injectFields(Object testInstance) {
        for (Field f : testInstance.getClass().getDeclaredFields()) {
            InstanceContext<?, ?> instance = getDeployedInstance(f.getType(), f.getAnnotations());
            if(instance == null) { // a test class might have fields not meant for injection
                continue;
            }
            try {
                f.setAccessible(true);
                f.set(testInstance, instance.getValue());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void afterAll() {
        LOGGER.trace("Closing instances with class lifecycle");
        List<InstanceContext<?, ?>> destroy = deployedInstances.stream().filter(i -> i.getLifeCycle().equals(LifeCycle.CLASS)).toList();
        destroy.forEach(this::destroy);
    }

    public void afterEach() {
        LOGGER.trace("Closing instances with method lifecycle");
        List<InstanceContext<?, ?>> destroy = deployedInstances.stream().filter(i -> i.getLifeCycle().equals(LifeCycle.METHOD)).toList();
        destroy.forEach(this::destroy);
    }

    public void close() {
        LOGGER.trace("Closing all instances");
        List<InstanceContext<?, ?>> destroy = deployedInstances.stream().toList();
        destroy.forEach(this::destroy);
    }

    List<Supplier<?, ?>> getSuppliers() {
        return suppliers;
    }

    private RequestedInstance<?, ?> createRequestedInstance(Annotation[] annotations, Class<?> valueType) {
        for (Annotation a : annotations) {
            for (Supplier s : suppliers) {
                if (s.getAnnotationClass().equals(a.annotationType())) {
                    return new RequestedInstance(s, a, valueType);
                }
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
                        && Objects.equals(supplier.getRef(a), i.getRef()) ) {
                    return i;
                }
            }
        }
        return null;
    }

    private void destroy(InstanceContext instanceContext) {
        boolean removed = deployedInstances.remove(instanceContext);
        if (removed) {
            Set<InstanceContext> dependencies = instanceContext.getDependencies();
            dependencies.forEach(this::destroy);
            instanceContext.getSupplier().close(instanceContext);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Closed instance: {0}",
                        instanceContext.getSupplier().getClass().getSimpleName());
            }
        }
    }

    private InstanceContext getDeployedInstance(RequestedInstance requestedInstance) {
        String requestedRef = requestedInstance.getRef();
        Class requestedValueType = requestedInstance.getValueType();
        for (InstanceContext<?, ?> i : deployedInstances) {
            if(!Objects.equals(i.getRef(), requestedRef)) {
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

    private void loadSuppliers() {
        Iterator<Supplier> supplierIterator = ServiceLoader.load(Supplier.class).iterator();
        Set<Class> loadedValueTypes = new HashSet<>();
        Set<Supplier> skippedSuppliers = new HashSet<>();

        while (supplierIterator.hasNext()) {
            Supplier supplier = supplierIterator.next();
            boolean shouldAdd = false;
            Class supplierValueType = supplier.getValueType();

            if (!loadedValueTypes.contains(supplierValueType)) {
                String requestedSupplier = Config.getSelectedSupplier(supplierValueType);
                if (requestedSupplier != null) {
                    if (requestedSupplier.equals(supplier.getAlias())) {
                        shouldAdd = true;
                    }
                } else {
                    shouldAdd = true;
                }
            }

            if (shouldAdd) {
                suppliers.add(supplier);
                loadedValueTypes.add(supplierValueType);
            } else {
                skippedSuppliers.add(supplier);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            StringBuilder loaded = new StringBuilder();
            loaded.append("Loaded suppliers:");
            for (Supplier s : suppliers) {
                loaded.append("\n - " + ValueTypeAlias.getAlias(s.getValueType()) + " --> " + s.getAlias());
            }
            LOGGER.trace(loaded.toString());

            StringBuilder skipped = new StringBuilder();
            skipped.append("Skipped suppliers:");
            for (Supplier s : skippedSuppliers) {
                skipped.append("\n - " + ValueTypeAlias.getAlias(s.getValueType()) + " --> " + s.getAlias());
            }
            LOGGER.trace(skipped.toString());
        }
    }

    private InstanceContext getDeployedInstance(Class typeClass, String ref) {
        return deployedInstances.stream()
                .filter(i -> i.getSupplier().getValueType().equals(typeClass) && Objects.equals(i.getRef(), ref))
                .findFirst().orElse(null);
    }

    private RequestedInstance getRequestedInstance(Class typeClass, String ref) {
        return requestedInstances.stream()
                .filter(i -> i.getSupplier().getValueType().equals(typeClass) && Objects.equals(i.getRef(), ref))
                .findFirst().orElse(null);
    }

    private void invokeBeforeEachOnSuppliers() {
        for (InstanceContext i : deployedInstances) {
            i.getSupplier().onBeforeEach(i);
        }
    }

}
