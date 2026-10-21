package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.testframework.injection.predicates.RequestedInstancePredicates;

import org.jboss.logging.Logger;

public class DependencyGraphResolver {

    private static final Logger log = Logger.getLogger(DependencyGraphResolver.class);

    private final Registry registry;
    private final List<RequestedInstance<?, ?>> missingInstances;

    private Set<Dependency> visited = new HashSet<>();
    private Set<Dependency> visiting = new HashSet<>();

    public DependencyGraphResolver(Registry registry) {
        this.registry = registry;
        this.missingInstances = new LinkedList<>();

        for (RequestedInstance requestedInstance : registry.getRequestedInstances()) {
            List<Dependency> dependencies = requestedInstance.getSupplier().getDependencies(requestedInstance);
            requestedInstance.setDeclaredDependencies(dependencies);
            for (Dependency dependency : dependencies) {
                scan(dependency);
            }
        }
    }

    public List<RequestedInstance<?, ?>> getMissingInstances() {
        return missingInstances;
    }

    private void scan(Dependency dependency) {
        if (visited.contains(dependency)) {
            log.tracev("Skipping {0} already scanned", dependency);
        } else {
            log.tracev("Scanning dependency {0}", dependency);
        }

        if (visiting.contains(dependency)) {
            throw new RuntimeException("Dependency cycle detected in " + visiting.stream().map(Dependency::toString).collect(Collectors.joining(", ")));
        }

        visiting.add(dependency);

        RequestedInstance matchingInstance = registry.getRequestedInstances().stream().filter(RequestedInstancePredicates.matches(dependency.valueType(), dependency.ref())).findFirst().orElse(null);
        if (matchingInstance == null) {
            matchingInstance = missingInstances.stream().filter(RequestedInstancePredicates.matches(dependency.valueType(), dependency.ref())).findFirst().orElse(null);
        }

        if (matchingInstance == null) {
            Supplier<?, ?> supplier = registry.getExtensions().findSupplierByType(dependency.valueType());
            Annotation defaultAnnotation = DefaultAnnotationProxy.proxy(supplier.getAnnotationClass(), dependency.ref());
            matchingInstance = registry.createRequestedInstance(new Annotation[]{ defaultAnnotation }, dependency.valueType());
            missingInstances.add(matchingInstance);
        }

        List<Dependency> dependencies = matchingInstance.getSupplier().getDependencies(matchingInstance);
        matchingInstance.setDeclaredDependencies(dependencies);

        dependencies.forEach(this::scan);

        visiting.remove(dependency);
        visited.add(dependency);
    }
}
