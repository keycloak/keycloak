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
import java.util.Optional;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.keycloak.storage.ldap.idm.model.LDAPDn;

/**
 * The names of the LDAP groups a user is member of, read from the values of the user's membership attribute.
 *
 * @param names the group names
 * @param unreadable the DN values whose first RDN does not carry the group name attribute
 */
record LDAPGroupNames(Set<String> names, Set<String> unreadable) {

    public LDAPGroupNames {
        names = Set.copyOf(names);
        unreadable = Set.copyOf(unreadable);
    }

    /**
     * A DN value is read as the value of {@code groupNameAttribute} in its first RDN. Any other value is the group name itself.
     */
    static LDAPGroupNames of(Collection<String> membershipValues, String groupNameAttribute) {
        NameLookup<String> namesByValue = NameLookup.of(nonBlank(membershipValues), value -> groupNameOf(value, groupNameAttribute));
        return new LDAPGroupNames(namesByValue.found(), namesByValue.missing());
    }

    private static List<String> nonBlank(Collection<String> values) {
        return values.stream().filter(value -> !value.isBlank()).toList();
    }

    private static Optional<String> groupNameOf(String membershipValue, String groupNameAttribute) {
        return asDn(membershipValue)
                .map(dn -> nameInFirstRdn(dn, groupNameAttribute))
                .orElse(Optional.of(membershipValue));
    }

    private static Optional<String> nameInFirstRdn(LDAPDn dn, String groupNameAttribute) {
        return Optional.ofNullable(dn.getFirstRdn()).map(rdn -> rdn.getAttrValue(groupNameAttribute));
    }

    private static Optional<LDAPDn> asDn(String value) {
        try {
            return Optional.of(LDAPDn.fromLdapName(new LdapName(value)));
        } catch (InvalidNameException e) {
            return Optional.empty();
        }
    }
}
