package org.keycloak.compatibility;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class Util {

    private Util() {
    }

    public static <T> Stream<T> mergeKeySet(Map<T, ?> map1, Map<T, ?> map2) {
        return Stream.concat(
                map1.keySet().stream(),
                map2.keySet().stream()
        ).distinct();
    }

    public static CompatibilityResult isCompatible(String provider, Map<String, String> old, Map<String, String> current) {
        return mergeKeySet(old, current)
                .sorted()
                .map(key -> compare(provider, key, old.get(key), current.get(key)))
                .filter(Util::isNotCompatible)
                .reduce((a, b) -> {
                    if (! (a instanceof AggregatedCompatibilityResult)) {
                        a = new AggregatedCompatibilityResult(a);
                    }

                    return ((AggregatedCompatibilityResult) a).add(b);
                })
                .orElse(CompatibilityResult.providerCompatible(provider));
    }

    public static boolean isNotCompatible(CompatibilityResult result) {
        return result.exitCode() != CompatibilityResult.ExitCode.ROLLING.value();
    }

    private static CompatibilityResult compare(String provider, String key, String old, String current) {
        return Objects.equals(old, current) ?
                CompatibilityResult.providerCompatible(provider) :
                CompatibilityResult.incompatibleAttribute(provider, key, old, current);
    }

}
