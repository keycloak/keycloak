package org.keycloak.testframework.injection;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

@SuppressWarnings("rawtypes")
class RegistryLogger {

    private static final Logger LOGGER = Logger.getLogger(RegistryLogger.class);
    private final ValueTypeAlias valueTypeAlias;

    public RegistryLogger(ValueTypeAlias valueTypeAlias) {
        this.valueTypeAlias = valueTypeAlias;
    }

    public void logDependencyInjection(InstanceContext<?, ?> dependent, InstanceContext<?, ?> dependency, InjectionType injectionType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Injecting {0} dependency {1}#{2,number,#} into {3}#{4,number,#}",
                    injectionType,
                    dependency.getSupplier().getClass().getSimpleName(),
                    dependency.getInstanceId(),
                    dependent.getSupplier().getClass().getSimpleName(),
                    dependent.getInstanceId());
        }
    }

    public void logRequestedInstances(List<RequestedInstance<?, ?>> requestedInstances) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Requested instances: {0}",
                    requestedInstances.stream().map(r -> r.getSupplier().getValueType().getSimpleName()).collect(Collectors.joining(", ")));
        }
    }

    public void logReusingCompatibleInstance(InstanceContext instance) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Reusing compatible: {0}#{1,number,#}",
                    instance.getSupplier().getClass().getSimpleName(),
                    instance.getInstanceId());
        }
    }

    public void logDestroy(InstanceContext instance) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Closed instance: {0}#{1,number,#}",
                    instance.getSupplier().getClass().getSimpleName(),
                    instance.getInstanceId());
        }
    }

    public void logDestroyIncompatible(InstanceContext instance) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Closing non-compatible instance: {0}#{1,number,#}",
                    instance.getSupplier().getClass().getSimpleName(),
                    instance.getInstanceId());
        }
    }

    public void logDestroyDirty(InstanceContext instance) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Closing dirty instance: {0}#{1,number,#}",
                    instance.getSupplier().getClass().getSimpleName(),
                    instance.getInstanceId());
        }
    }

    public void logCleanup(InstanceContext instance) {
        LOGGER.debugv("Cleanup instance {0}#{1,number,#}", instance.getValue(), instance.getInstanceId());
    }

    public void logCreatedInstance(RequestedInstance requestedInstance, InstanceContext instance) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Created instance: {0}#{1,number,#}",
                    requestedInstance.getSupplier().getClass().getSimpleName(), instance.getInstanceId());
        }
    }

    public void logAfterAll() {
        LOGGER.debug("Closing instances with class lifecycle");
    }

    public void logAfterEach() {
        LOGGER.debug("Closing instances with method lifecycle");
    }

    public void logClose() {
        LOGGER.debug("Closing all instances");
    }

    public void logSuppliers(List<Supplier<?, ?>> suppliers, List<Supplier<?, ?>> skippedSuppliers) {
        if (LOGGER.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Loaded suppliers:");
            for (Supplier s : suppliers) {
                sb.append("\n - ");
                appendSupplierInfo(s, sb);
            }

            sb.append("\nSkipped suppliers:");
            for (Supplier s : skippedSuppliers) {
                sb.append("\n - ");
                appendSupplierInfo(s, sb);
            }

            LOGGER.debug(sb.toString());
        }
    }

    private void appendSupplierInfo(Supplier s, StringBuilder sb) {
        sb.append("supplierType=");
        sb.append(s.getClass().getSimpleName());
        sb.append(", valueType=");
        sb.append(s.getValueType().getSimpleName());

        String alias = valueTypeAlias.getAlias(s.getValueType());
        if (!alias.equals(s.getValueType().getSimpleName())) {
            sb.append(", alias=");
            sb.append(alias);
        }

    }

    public void logIntercepted(Object value, Supplier<?, ?> supplier) {
        LOGGER.debugv("{0} intercepted by {1}", value.getClass().getSimpleName(), supplier.getClass().getSimpleName());
    }

    public enum InjectionType {

        EXISTING("existing"),
        REQUESTED("requested"),
        UN_CONFIGURED("un-configured");

        private String stringRep;

        InjectionType(String stringRep) {
            this.stringRep = stringRep;
        }


        @Override
        public String toString() {
            return stringRep;
        }
    }

}
