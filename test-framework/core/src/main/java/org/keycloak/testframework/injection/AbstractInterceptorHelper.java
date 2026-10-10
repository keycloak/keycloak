package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class to apply interceptors to a value while an instance is being created.
 * <p>
 * On construction all deployed and requested instances of the registry whose supplier implements the given
 * interceptor interface are collected. {@link #intercept(Object, InstanceContext)} then passes the value through
 * every collected interceptor, which allows a supplier to contribute to the configuration of another instance it
 * depends on.
 * <p>
 * Interceptors are collected regardless of whether they have already been deployed, as an interceptor usually
 * depends on the instance it intercepts and is therefore deployed later.
 *
 * @param <I> the interceptor interface implemented by a supplier
 * @param <V> the type of the value passed through the interceptors
 */
public abstract class AbstractInterceptorHelper<I, V> {

    private final Registry registry;
    private final Class<?> interceptorClass;
    private final List<Interception> interceptions = new LinkedList<>();

    public AbstractInterceptorHelper(Registry registry, Class<I> interceptorClass) {
        this.registry = registry;
        this.interceptorClass = interceptorClass;

        registry.getDeployedInstances().stream().filter(i -> isInterceptor(i.getSupplier())).forEach(i -> interceptions.add(new Interception(i)));
        registry.getRequestedInstances().stream().filter(r -> isInterceptor(r.getSupplier())).forEach(r -> interceptions.add(new Interception(r)));
    }

    /**
     * Passes the value through all applicable interceptors, in the order they were collected. Each interceptor that
     * is applied registers the intercepted instance as its dependent, so the interceptor is destroyed before the
     * instance it contributed to.
     *
     * @param value the value to intercept, for example the builder of the instance being created
     * @param instanceContext the instance context of the instance being created
     * @return the value as returned by the last applicable interceptor, or the given value if none applies
     */
    public V intercept(V value, InstanceContext<?, ?> instanceContext) {
        for (Interception interception : interceptions) {
            if (!applies(interception, instanceContext)) {
                continue;
            }

            value = intercept(value, interception.getSupplier(), interception.getExistingInstance());
            registry.getLogger().logIntercepted(value, interception.getSupplier());
            if (interception.getExistingInstance() != null) {
                interception.getExistingInstance().registerDependent(instanceContext);
            } else {
                interception.getRequestedInstance().registerDependent(instanceContext);
            }
        }
        return value;
    }

    /**
     * Invokes the interceptor method of the given supplier. Implemented by subclasses as the interceptor interface,
     * and with it the method to call, is specific to the intercepted value.
     *
     * @param value the value to intercept
     * @param supplier the supplier implementing the interceptor interface
     * @param existingInstance the instance context of the interceptor, or <code>null</code> if it is not deployed yet
     * @return the intercepted value
     */
    public abstract V intercept(V value, Supplier<?, ?> supplier, InstanceContext<?, ?> existingInstance);

    /**
     * Controls whether the given interception applies to the instance currently being created. Returning
     * <code>false</code> skips the interception entirely, including registering the intercepted instance as a
     * dependent of the interceptor.
     *
     * @param interception the interception to check
     * @param target the instance context for the instance being intercepted
     * @return <code>true</code> if the interception should be applied
     */
    protected boolean applies(Interception interception, InstanceContext<?, ?> target) {
        return true;
    }

    /**
     * Checks whether the given supplier implements the interceptor interface this helper was created for.
     *
     * @param supplier the supplier to check
     * @return <code>true</code> if the supplier is an interceptor
     */
    private boolean isInterceptor(Supplier<?, ?> supplier) {
        return interceptorClass.isAssignableFrom(supplier.getClass());
    }

    /**
     * A single interceptor collected by {@link AbstractInterceptorHelper}.
     * <p>
     * An interceptor is either already deployed or only requested so far, which is why either
     * {@link #getExistingInstance()} or {@link #getRequestedInstance()} is set, but never both. The supplier and the
     * annotation are available in both cases and can be used by {@link #applies(Interception, InstanceContext)} to
     * decide whether the interceptor applies to the instance being created.
     */
    public static class Interception {

        private final Supplier<?, ?> supplier;
        private final Annotation annotation;
        private final RequestedInstance<?, ?> requestedInstance;
        private final InstanceContext<?, ?> existingInstance;

        public Interception(InstanceContext<?, ?> existingInstance) {
            this.supplier = existingInstance.getSupplier();
            this.annotation = existingInstance.getAnnotation();
            this.requestedInstance = null;
            this.existingInstance = existingInstance;
        }

        public Interception(RequestedInstance<?, ?> requestedInstance) {
            this.supplier = requestedInstance.getSupplier();
            this.annotation = requestedInstance.getAnnotation();
            this.requestedInstance = requestedInstance;
            this.existingInstance = null;
        }

        /**
         * The supplier of the interceptor, which implements the interceptor interface this helper was created for.
         *
         * @return the interceptor supplier
         */
        public Supplier<?, ?> getSupplier() {
            return supplier;
        }

        /**
         * The annotation of the interceptor itself. Unlike {@link #getExistingInstance()} this is always available,
         * whether the interceptor has been deployed yet.
         *
         * @return the interceptor annotation
         */
        public Annotation getAnnotation() {
            return annotation;
        }

        /**
         * The instance context of the interceptor, or <code>null</code> if the interceptor has not been deployed yet.
         *
         * @return the interceptor instance context
         */
        public InstanceContext<?, ?> getExistingInstance() {
            return existingInstance;
        }

        /**
         * The requested instance of the interceptor, or <code>null</code> if the interceptor is already deployed.
         *
         * @return the requested interceptor instance
         */
        public RequestedInstance<?, ?> getRequestedInstance() {
            return requestedInstance;
        }
    }

}
