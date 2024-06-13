package org.keycloak.test.framework.injection;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Registry {

    private static final Logger LOGGER = Logger.getLogger(Registry.class);

    private ExtensionContext currentContext;
    private final List<Supplier<?, ?>> suppliers = new LinkedList<>();
    private final List<InstanceWrapper<?, ?>> deployedInstances = new LinkedList<>();
    private final List<InstanceWrapper<?, ?>> requestedInstances = new LinkedList<>();

    public Registry() {
        loadSuppliers();
    }

    public ExtensionContext getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(ExtensionContext currentContext) {
        this.currentContext = currentContext;
    }

    public <T> T getDependency(Class<T> typeClass, InstanceWrapper dependent) {
        InstanceWrapper dependency = getDeployedInstance(typeClass);
        if (dependency != null) {
            dependency.registerDependency(dependent);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Injecting existing dependency {0} into {1}",
                        dependency.getSupplier().getClass().getSimpleName(),
                        dependent.getSupplier().getClass().getSimpleName());
            }

            return (T) dependency.getValue();
        }

        dependency = getRequestedInstance(typeClass);
        if (dependency != null) {
            dependency = dependency.getSupplier().getValue(this, dependency.getAnnotation());
            dependency.registerDependency(dependent);
            deployedInstances.add(dependency);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Injecting requested dependency {0} into {1}",
                        dependency.getSupplier().getClass().getSimpleName(),
                        dependent.getSupplier().getClass().getSimpleName());
            }

            return (T) dependency.getValue();
        }

        Optional<Supplier<?, ?>> supplied = suppliers.stream().filter(s -> s.getValueType().equals(typeClass)).findFirst();
        if (supplied.isPresent()) {
            Supplier<?, ?> supplier = supplied.get();
            dependency = supplier.getValue(this, null);
            deployedInstances.add(dependency);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Injecting un-configured dependency {0} into {1}",
                        dependency.getSupplier().getClass().getSimpleName(),
                        dependent.getSupplier().getClass().getSimpleName());
            }

            return (T) dependency.getValue();
        }

        throw new RuntimeException("Dependency not found: " + typeClass);
    }

    public void beforeAll(Class testClass) {
        InstanceWrapper requestedServerInstance = createInstanceWrapper(testClass.getAnnotations());
        requestedInstances.add(requestedServerInstance);

        for (Field f : testClass.getDeclaredFields()) {
            InstanceWrapper instanceWrapper = createInstanceWrapper(f.getAnnotations());
            if (instanceWrapper != null) {
                requestedInstances.add(instanceWrapper);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracev("Requested suppliers: {0}",
                    requestedInstances.stream().map(r -> r.getSupplier().getClass().getSimpleName()).collect(Collectors.joining(", ")));
        }

        Iterator<InstanceWrapper<?, ?>> itr = requestedInstances.iterator();
        while (itr.hasNext()) {
            InstanceWrapper<?, ?> requestedInstance = itr.next();
            InstanceWrapper deployedInstance = getDeployedInstance(requestedInstance.getSupplier());
            if (deployedInstance != null) {
                if (deployedInstance.getSupplier().compatible(deployedInstance, requestedInstance)) {
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

        itr = requestedInstances.iterator();
        while (itr.hasNext()) {
            InstanceWrapper requestedInstance = itr.next();

            InstanceWrapper instance = requestedInstance.getSupplier().getValue(this, requestedInstance.getAnnotation());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Created instance: {0}",
                        requestedInstance.getSupplier().getClass().getSimpleName());
            }

            deployedInstances.add(instance);

            itr.remove();
        }

    }

    public void beforeEach(Object testInstance) {
        for (Field f : testInstance.getClass().getDeclaredFields()) {
            InstanceWrapper<?, ?> instance = getDeployedInstance(f.getAnnotations());
            try {
                f.setAccessible(true);
                f.set(testInstance, instance.getValue());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void afterAll() {
        List<InstanceWrapper<?, ?>> destroy = deployedInstances.stream().filter(i -> i.getSupplier().getLifeCycle().equals(LifeCycle.CLASS)).toList();
        destroy.forEach(this::destroy);
    }

    private InstanceWrapper<?, ?> createInstanceWrapper(Annotation[] annotations) {
        for (Annotation a : annotations) {
            for (Supplier s : suppliers) {
                if (s.getAnnotationClass().equals(a.annotationType())) {
                    return new InstanceWrapper(s, a);
                }
            }
        }
        return null;
    }

    private InstanceWrapper<?, ?> getDeployedInstance(Annotation[] annotations) {
        for (Annotation a : annotations) {
            for (InstanceWrapper<?, ?> i : deployedInstances) {
                if (i.getSupplier().getAnnotationClass().equals(a.annotationType())) {
                    return i;
                }
            }
        }
        return null;
    }

    private void destroy(InstanceWrapper instanceWrapper) {
        boolean removed = deployedInstances.remove(instanceWrapper);
        if (removed) {
            Set<InstanceWrapper> dependencies = instanceWrapper.getDependencies();
            dependencies.forEach(this::destroy);
            instanceWrapper.getSupplier().close(instanceWrapper.getValue());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracev("Closed instance: {0}",
                        instanceWrapper.getSupplier().getClass().getSimpleName());
            }
        }
    }

    private InstanceWrapper getDeployedInstance(Supplier supplier) {
        return deployedInstances.stream().filter(i -> i.getSupplier().equals(supplier)).findFirst().orElse(null);
    }

    private void loadSuppliers() {
        ServiceLoader.load(Supplier.class).iterator().forEachRemaining(suppliers::add);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracev("Suppliers: {0}", suppliers.stream().map(s -> s.getClass().getSimpleName()).collect(Collectors.joining(", ")));
        }
    }

    private InstanceWrapper getDeployedInstance(Class typeClass) {
        return deployedInstances.stream().filter(i -> i.getSupplier().getValueType().equals(typeClass)).findFirst().orElse(null);
    }

    private InstanceWrapper getRequestedInstance(Class typeClass) {
        return requestedInstances.stream().filter(i -> i.getSupplier().getValueType().equals(typeClass)).findFirst().orElse(null);
    }

}
