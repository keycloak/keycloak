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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * What a translated name denotes.
 */
public sealed interface GrantTarget {

    String REALM_ROLE = "REALM_ROLE";
    String CLIENT_ROLE = "CLIENT_ROLE";
    String GROUP = "GROUP";

    Grants resolve(KeycloakSession session, RealmModel realm, Collection<String> names);

    /**
     * How an administrator would refer to the names this target looks up, for log messages.
     */
    String describe();

    /**
     * Why no name can resolve in the realm, for configuration validation.
     */
    Optional<String> problemIn(RealmModel realm);

    record RealmRole() implements GrantTarget {

        @Override
        public Grants resolve(KeycloakSession session, RealmModel realm, Collection<String> names) {
            return Grants.roles(names, realm::getRole);
        }

        @Override
        public String describe() {
            return "realm roles";
        }

        @Override
        public Optional<String> problemIn(RealmModel realm) {
            return Optional.empty();
        }
    }

    record ClientRole(String clientId) implements GrantTarget {

        @Override
        public Grants resolve(KeycloakSession session, RealmModel realm, Collection<String> names) {
            return client(realm)
                    .map(client -> Grants.roles(names, client::getRole))
                    .orElseGet(() -> Grants.unresolved(names));
        }

        @Override
        public String describe() {
            return "roles of client '" + clientId + "'";
        }

        @Override
        public Optional<String> problemIn(RealmModel realm) {
            return client(realm).isEmpty() ? Optional.of(noSuchClientMessage()) : Optional.empty();
        }

        private String noSuchClientMessage() {
            return "There is no client '" + clientId + "' in the realm.";
        }

        private Optional<ClientModel> client(RealmModel realm) {
            return Optional.ofNullable(realm.getClientByClientId(clientId));
        }
    }

    /**
     * Names are full group paths, such as {@code /parent/child}.
     */
    record Group() implements GrantTarget {

        @Override
        public Grants resolve(KeycloakSession session, RealmModel realm, Collection<String> names) {
            return Grants.groups(names, path -> KeycloakModelUtils.findGroupByPath(session, realm, path));
        }

        @Override
        public String describe() {
            return "groups";
        }

        @Override
        public Optional<String> problemIn(RealmModel realm) {
            return Optional.empty();
        }
    }

    /**
     * @param roles the roles the names resolved to
     * @param groups the groups the names resolved to
     * @param unresolved the names nothing in the realm answers to
     */
    record Grants(Set<RoleModel> roles, Set<GroupModel> groups, Set<String> unresolved) {

        public Grants {
            roles = Set.copyOf(roles);
            groups = Set.copyOf(groups);
            unresolved = Set.copyOf(unresolved);
        }

        private static Grants unresolved(Collection<String> names) {
            return new Grants(Set.of(), Set.of(), Set.copyOf(names));
        }

        private static Grants roles(Collection<String> names, Function<String, RoleModel> lookup) {
            NameLookup<RoleModel> byName = NameLookup.of(names, name -> Optional.ofNullable(lookup.apply(name)));
            return new Grants(byName.found(), Set.of(), byName.missing());
        }

        private static Grants groups(Collection<String> paths, Function<String, GroupModel> lookup) {
            NameLookup<GroupModel> byPath = NameLookup.of(paths, path -> Optional.ofNullable(lookup.apply(path)));
            return new Grants(Set.of(), byPath.found(), byPath.missing());
        }
    }
}
