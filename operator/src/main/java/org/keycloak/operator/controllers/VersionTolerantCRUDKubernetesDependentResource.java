package org.keycloak.operator.controllers;

import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

public class VersionTolerantCRUDKubernetesDependentResource<R extends HasMetadata, P extends HasMetadata>
        extends CRUDKubernetesDependentResource<R, P> {
    
    public VersionTolerantCRUDKubernetesDependentResource() {
        super();
    }

    public VersionTolerantCRUDKubernetesDependentResource(Class<R> clazz) {
        super(clazz);
    }

    /**
     * Overrides the default handling to use startsWith api, rather than equality on the apiVersion
     */
    @Override
    protected Optional<SecondaryToPrimaryMapper<R>> getSecondaryToPrimaryMapper(EventSourceContext<P> context) {
        return primaryMapper(context);
    }

    static <R extends HasMetadata, P extends HasMetadata> Optional<SecondaryToPrimaryMapper<R>> primaryMapper(EventSourceContext<P> context) {
        Class<?> primaryClass = context.getPrimaryResourceClass();
        String apiVersion = HasMetadata.getApiVersion(primaryClass);
        String kind = HasMetadata.getKind(primaryClass);
        apiVersion = apiVersion.startsWith("/") ? apiVersion.substring(1) : apiVersion;
        String correctApi = apiVersion.substring(0, apiVersion.indexOf("/") + 1);
        boolean clusterScoped = !Namespaced.class.isAssignableFrom(primaryClass);
        return Optional.of(resource -> resource.getMetadata().getOwnerReferences().stream()
                .filter(owner -> owner.getKind().equals(kind) && owner.getApiVersion().startsWith(correctApi))
                .map(or -> ResourceID.fromOwnerReference(resource, or, clusterScoped)).collect(Collectors.toSet()));
    }

}
