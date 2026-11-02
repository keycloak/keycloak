/*
 * Copyright 2026 Andrzej Świętek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.storage.ldap.mappers.membership.translation;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * What each of a set of names was looked up to.
 */
record NameLookup<T>(Map<String, Optional<T>> results) {

    NameLookup {
        results = Map.copyOf(results);
    }

    static <T> NameLookup<T> of(Collection<String> names, Function<String, Optional<T>> lookup) {
        return new NameLookup<>(names.stream().distinct().collect(Collectors.toUnmodifiableMap(Function.identity(), lookup)));
    }

    Set<T> found() {
        return results.values().stream().flatMap(Optional::stream).collect(Collectors.toUnmodifiableSet());
    }

    Set<String> missing() {
        return results.entrySet().stream().filter(entry -> entry.getValue().isEmpty()).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }
}
