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

package org.keycloak.broker.provider.mappersync;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * The role a role reference names, resolved by the rules of
 * {@link KeycloakModelUtils#getRoleFromString(org.keycloak.models.KeycloakSession, org.keycloak.models.RealmModel, String)}:
 * the longest prefix that is an existing client ID names the client, and a reference without such a prefix names a
 * realm role.
 *
 * @param clientId the client of a client role; empty for a realm role
 */
record RoleReference(Optional<String> clientId, String roleName) {

    /**
     * The role {@code reference} names in a realm whose client IDs are those {@code isClientId} accepts; empty when the
     * reference has more client ID candidates than Keycloak looks up.
     */
    static Optional<RoleReference> resolve(String reference, Predicate<String> isClientId) {
        List<Integer> separators = separatorsFromLast(reference);
        return clientRole(reference, separators, isClientId).or(() -> realmRole(reference, separators));
    }

    private static Optional<RoleReference> clientRole(String reference, List<Integer> separators, Predicate<String> isClientId) {
        return separators.stream()
                .limit(KeycloakModelUtils.MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE)
                .filter(separator -> isClientId.test(reference.substring(0, separator)))
                .findFirst()
                .map(separator -> splitAt(reference, separator));
    }

    private static RoleReference splitAt(String reference, int separator) {
        return new RoleReference(Optional.of(reference.substring(0, separator)), reference.substring(separator + 1));
    }

    private static Optional<RoleReference> realmRole(String reference, List<Integer> separators) {
        return separators.size() < KeycloakModelUtils.MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE
                ? Optional.of(new RoleReference(Optional.empty(), reference))
                : Optional.empty();
    }

    private static List<Integer> separatorsFromLast(String reference) {
        return IntStream.iterate(reference.length() - 1, index -> index >= 0, index -> index - 1)
                .filter(index -> reference.charAt(index) == KeycloakModelUtils.CLIENT_ROLE_SEPARATOR)
                .boxed()
                .toList();
    }
}
