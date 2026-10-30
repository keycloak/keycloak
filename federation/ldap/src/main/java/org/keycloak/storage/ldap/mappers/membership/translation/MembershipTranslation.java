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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Turns LDAP group names into the names of roles or groups. A table entry for a name wins over the expression.
 */
record MembershipTranslation(Map<String, List<String>> table, Optional<NameExpression> expression) {

    public MembershipTranslation {
        table = Map.copyOf(table);
    }

    Translated translateAll(Collection<String> groupNames) {
        Map<Boolean, Set<String>> byTableEntry = partitionByTableEntry(groupNames);
        NameLookup<String> byExpression = NameLookup.of(byTableEntry.get(false), this::translateByExpression);
        return new Translated(translateByTable(byTableEntry.get(true)), byExpression.found(), byExpression.missing());
    }

    private Map<Boolean, Set<String>> partitionByTableEntry(Collection<String> groupNames) {
        return groupNames.stream().collect(Collectors.partitioningBy(table::containsKey, Collectors.toUnmodifiableSet()));
    }

    private Set<String> translateByTable(Set<String> groupNames) {
        return groupNames.stream().map(table::get).flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
    }

    private Optional<String> translateByExpression(String groupName) {
        return expression.flatMap(e -> e.translate(groupName));
    }

    /**
     * @param fromTable the role or group names that table entries translated LDAP groups to
     * @param fromExpression the role or group names that the expression translated LDAP groups to
     * @param untranslated the LDAP group names neither the table nor the expression translated
     */
    record Translated(Set<String> fromTable, Set<String> fromExpression, Set<String> untranslated) {

        public Translated {
            fromTable = Set.copyOf(fromTable);
            fromExpression = Set.copyOf(fromExpression);
            untranslated = Set.copyOf(untranslated);
        }

        public Set<String> names() {
            return Stream.concat(fromTable.stream(), fromExpression.stream()).collect(Collectors.toUnmodifiableSet());
        }
    }
}
