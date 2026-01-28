package org.keycloak.compatibility;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

record AggregatedCompatibilityResult(Set<CompatibilityResult> compatibilityResults) implements CompatibilityResult {

    public AggregatedCompatibilityResult(CompatibilityResult compatibilityResult) {
        this(new HashSet<>());
        this.compatibilityResults.add(compatibilityResult);
    }

    public AggregatedCompatibilityResult add(CompatibilityResult a) {
        compatibilityResults.add(a);
        return this;
    }

    @Override
    public int exitCode() {
        return compatibilityResults.stream()
                .anyMatch(r -> r.exitCode() == ExitCode.RECREATE.value())
                ? ExitCode.RECREATE.value() : ExitCode.ROLLING.value();
    }

    @Override
    public Optional<String> errorMessage() {
        StringBuilder sb = new StringBuilder("Aggregated incompatible results:\n");
        for (CompatibilityResult result : compatibilityResults) {
            sb.append(result.errorMessage()).append("\n");
        }
        return Optional.of(sb.toString());
    }

    @Override
    public Optional<Set<String>> incompatibleAttributes() {
        return Optional.of(compatibilityResults.stream()
                .filter(r -> ProviderIncompatibleResult.class.isAssignableFrom(r.getClass()))
                .map(ProviderIncompatibleResult.class::cast)
                .flatMap(r -> r.incompatibleAttributes().orElse(Set.of()).stream())
                .collect(Collectors.toSet()));
    }
}
