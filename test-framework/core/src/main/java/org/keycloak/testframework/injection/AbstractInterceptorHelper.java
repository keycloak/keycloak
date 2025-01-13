package org.keycloak.testframework.injection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractInterceptorHelper<I, V> {

    private final Registry registry;
    private final Class<?> interceptorClass;
    private final List<Interception> interceptions = new LinkedList<>();
    private final InterceptedBy interceptedBy = new InterceptedBy();

    public AbstractInterceptorHelper(Registry registry, Class<I> interceptorClass) {
        this.registry = registry;
        this.interceptorClass = interceptorClass;

        registry.getDeployedInstances().stream().filter(i -> isInterceptor(i.getSupplier())).forEach(i -> interceptions.add(new Interception(i)));
        registry.getRequestedInstances().stream().filter(r -> isInterceptor(r.getSupplier())).forEach(r -> interceptions.add(new Interception(r)));

        interceptions.forEach(i -> interceptedBy.put(i.supplier, i.instanceId));
    }

    public boolean sameInterceptors(InstanceContext<?, ?> instanceContext) {
        InterceptedBy previousInterceptedBy = instanceContext.getNote("InterceptedBy", InterceptedBy.class);
        return interceptedBy.equals(previousInterceptedBy);
    }

    public V intercept(V value, InstanceContext<?, ?> instanceContext) {
        for (Interception interception : interceptions) {
            value = intercept(value, interception.supplier, interception.existingInstance);
            registry.getLogger().logIntercepted(value, interception.supplier);
        }
        instanceContext.addNote("InterceptedBy", interceptedBy);
        return value;
    }

    public abstract V intercept(V value, Supplier<?, ?> supplier, InstanceContext<?, ?> existingInstance);

    private boolean isInterceptor(Supplier<?, ?> supplier) {
        return interceptorClass.isAssignableFrom(supplier.getClass());
    }

    public static class InterceptedBy extends HashMap<Supplier<?, ?>, Integer> {
    }

    private static class Interception {

        private final int instanceId;
        private final Supplier<?, ?> supplier;
        private final InstanceContext<?, ?> existingInstance;

        public Interception(InstanceContext<?, ?> existingInstance) {
            this.supplier = existingInstance.getSupplier();
            this.instanceId = existingInstance.getInstanceId();
            this.existingInstance = existingInstance;
        }

        public Interception(RequestedInstance<?, ?> requestedInstance) {
            this.supplier = requestedInstance.getSupplier();
            this.instanceId = requestedInstance.getInstanceId();
            this.existingInstance = null;
        }
    }

}
