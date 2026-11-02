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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.translation.GrantTarget.Grants;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Grants the roles or groups that the names of the user's LDAP groups translate to. They are resolved on the first role or group
 * access to a user loaded from LDAP.
 * Nothing is written to LDAP or stored in the database.
 */
public class TranslatedMembershipLDAPStorageMapper extends AbstractLDAPStorageMapper {

    private static final Logger logger = Logger.getLogger(TranslatedMembershipLDAPStorageMapper.class);

    private static final String UNREADABLE_GROUPS = "LDAP mapper '%1$s': no '%6$s' attribute in the first RDN of %3$s of user '%2$s', so these groups are ignored. Check the 'Group Name LDAP Attribute' of the mapper.";
    private static final String UNTRANSLATED_GROUPS = "LDAP mapper '%1$s': no translation for LDAP groups %3$s of user '%2$s'.";
    private static final String MISSING_FROM_TABLE = "LDAP mapper '%1$s': the translation table maps LDAP groups of user '%2$s' to %4$s %3$s, which do not exist in realm '%5$s', so they are not granted. Create them or correct the translation table.";
    private static final String MISSING_FROM_PATTERN = "LDAP mapper '%1$s': the translation pattern maps LDAP groups of user '%2$s' to %4$s %3$s, which do not exist in realm '%5$s', so they are not granted. Create them or correct the translation pattern or template.";

    private final TranslatedMembershipConfig config;

    public TranslatedMembershipLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, TranslatedMembershipConfig config) {
        super(mapperModel, ldapProvider);
        this.config = config;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        query.addReturningLdapAttribute(config.membershipAttribute());
        query.addReturningReadOnlyLdapAttribute(config.membershipAttribute());
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return new TranslatedMembershipUserDelegate(delegate, () -> grants(ldapUser, delegate, realm));
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
    }

    private Grants grants(LDAPObject ldapUser, UserModel user, RealmModel realm) {
        Set<String> memberships = LDAPUtils.getExistingMemberships(ldapProvider, config.membershipAttribute(), ldapUser);
        LDAPGroupNames groupNames = LDAPGroupNames.of(memberships, config.groupNameAttribute());
        MembershipTranslation.Translated translated = config.translation().translateAll(groupNames.names());
        Grants grants = config.target().resolve(getSession(), realm, translated.names());
        report(user, realm, groupNames, translated, grants);
        return grants;
    }

    private void report(UserModel user, RealmModel realm, LDAPGroupNames groupNames, MembershipTranslation.Translated translated, Grants grants) {
        Stream.of(
                        new Finding(Logger.Level.WARN, groupNames.unreadable(), UNREADABLE_GROUPS),
                        new Finding(Logger.Level.DEBUG, translated.untranslated(), UNTRANSLATED_GROUPS),
                        new Finding(Logger.Level.WARN, unresolvedFromTable(grants, translated), MISSING_FROM_TABLE),
                        new Finding(Logger.Level.WARN, unresolvedFromPattern(grants, translated), MISSING_FROM_PATTERN))
                .filter(finding -> logger.isEnabled(finding.level()) && !finding.names().isEmpty())
                .forEach(finding -> log(finding, user, realm));
    }

    private void log(Finding finding, UserModel user, RealmModel realm) {
        logger.logf(finding.level(), finding.format(),
                StringUtil.removeControlCharacters(mapperModel.getName()),
                StringUtil.removeControlCharacters(user.getUsername()),
                forLog(finding.names()),
                StringUtil.removeControlCharacters(config.target().describe()),
                StringUtil.removeControlCharacters(realm.getName()),
                StringUtil.removeControlCharacters(config.groupNameAttribute()));
    }

    private static Set<String> unresolvedFromTable(Grants grants, MembershipTranslation.Translated translated) {
        return grants.unresolved().stream().filter(translated.fromTable()::contains).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> unresolvedFromPattern(Grants grants, MembershipTranslation.Translated translated) {
        return grants.unresolved().stream().filter(name -> !translated.fromTable().contains(name)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The values sorted, quoted, and stripped of control characters.
     */
    static String forLog(Collection<String> values) {
        return values.stream().map(StringUtil::removeControlCharacters).sorted().map(value -> "'" + value + "'").collect(Collectors.joining(", "));
    }

    private class TranslatedMembershipUserDelegate extends UserModelDelegate {

        private final Supplier<Grants> grants;

        TranslatedMembershipUserDelegate(UserModel delegate, Supplier<Grants> resolve) {
            super(delegate);
            this.grants = new Memoized<>(resolve);
        }

        @Override
        public Stream<RoleModel> getRealmRoleMappingsStream() {
            return Stream.concat(super.getRealmRoleMappingsStream(), grantedRealmRoles()).distinct();
        }

        @Override
        public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
            return Stream.concat(super.getClientRoleMappingsStream(app), grantedRolesOf(app)).distinct();
        }

        @Override
        public Stream<RoleModel> getRoleMappingsStream() {
            return Stream.concat(super.getRoleMappingsStream(), grantedRoles()).distinct();
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return super.hasRole(role) || RoleUtils.hasRole(grantedRoles(), role) || RoleUtils.hasRoleFromGroup(grantedGroups(), role, true);
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {
            if (grants.get().roles().contains(role)) {
                throw new ModelException(grantedFromLDAP("Role", role.getName()));
            }
            super.deleteRoleMapping(role);
        }

        @Override
        public Stream<GroupModel> getGroupsStream() {
            return Stream.concat(super.getGroupsStream(), grantedGroups()).distinct();
        }

        @Override
        public boolean isMemberOf(GroupModel group) {
            return super.isMemberOf(group) || RoleUtils.isMember(grantedGroups(), group);
        }

        @Override
        public void leaveGroup(GroupModel group) {
            if (grants.get().groups().contains(group)) {
                throw new ModelException(grantedFromLDAP("Group", group.getName()));
            }
            super.leaveGroup(group);
        }

        private Stream<RoleModel> grantedRoles() {
            return grants.get().roles().stream();
        }

        private Stream<RoleModel> grantedRealmRoles() {
            return grantedRoles().filter(RoleUtils::isRealmRole);
        }

        private Stream<RoleModel> grantedRolesOf(ClientModel app) {
            return grantedRoles().filter(role -> RoleUtils.isClientRole(role, app));
        }

        private Stream<GroupModel> grantedGroups() {
            return grants.get().groups().stream();
        }

        private String grantedFromLDAP(String kind, String name) {
            return kind + " '" + StringUtil.removeControlCharacters(name) + "' is granted by LDAP mapper '" + StringUtil.removeControlCharacters(mapperModel.getName())
                    + "' from the user's LDAP groups. Remove the user from the LDAP group.";
        }
    }

    /**
     * The format takes the mapper, the user, the {@code names}, the target, the realm and the group name attribute as arguments 1 to 6.
     */
    private record Finding(Logger.Level level, Set<String> names, String format) {
    }

    /**
     * Computes the value on the first call and returns the same value afterwards. Not thread safe.
     */
    private static final class Memoized<T> implements Supplier<T> {

        private final Supplier<T> compute;
        private Optional<T> value = Optional.empty();

        Memoized(Supplier<T> compute) {
            this.compute = compute;
        }

        @Override
        public T get() {
            value = value.or(() -> Optional.of(compute.get()));
            return value.orElseThrow();
        }
    }
}
