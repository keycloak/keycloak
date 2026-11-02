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

package org.keycloak.broker.oidc.mappers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Invalid;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.RoleOutput;
import org.keycloak.broker.oidc.mappers.ClaimToGroupsAndRolesConfig.Valid;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.Mapping;
import org.keycloak.broker.oidc.mappers.DelimitedClaimEntries.Skipped;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.RoleGrantingIdentityProviderMapper;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * Maps a claim holding delimited entries, such as {@code tenant-a,admin,active}, to memberships of subgroups of a
 * parent group and to roles. On sync it revokes what it owns and the claim no longer grants: memberships of direct
 * subgroups of the parent group, and the roles named in its role table.
 */
public class ClaimToGroupsAndRolesMapper extends AbstractClaimMapper implements RoleGrantingIdentityProviderMapper {

    public static final String PROVIDER_ID = "oidc-claim-to-groups-and-roles-idp-mapper";

    private static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};

    private static final Logger LOG = Logger.getLogger(ClaimToGroupsAndRolesMapper.class);

    static final int MAX_LOGGED_LENGTH = 100;
    static final int MAX_LOGGED_ENTRIES = 3;

    private static final String REFUSED_GROUP_NAMES_SUMMARY = "skipped %d claim entries because their group name cannot be"
            + " used, so the user does not get their groups and roles: %s%s. Correct the entries at the identity provider"
            + " or the group field index.";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
                .name(ClaimToGroupsAndRolesConfig.CLAIM)
                .label("Claim")
                .helpText("Claim holding the entries, as a string or a list of strings. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.DELIMITER)
                .label("Field delimiter")
                .helpText("Single character separating the fields of an entry. Defaults to ','.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(ClaimToGroupsAndRolesConfig.DEFAULT_DELIMITER)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.STATE_FIELD_INDEX)
                .label("State field index")
                .helpText("Optional. Position of the state field in an entry, counting from 0. Entries whose state is not the required state are skipped.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.STATE_VALUE)
                .label("Required state")
                .helpText("State an entry must have to be mapped. Required when the state field index is set; compared exactly.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.GROUP_FIELD_INDEX)
                .label("Group field index")
                .helpText("Optional. Position of the group name field in an entry, counting from 0. The user becomes a member of the subgroup of that name under the parent group, which is created if missing. An entry whose group name is empty, longer than 255 characters, or contains '/', whitespace, or a control or formatting character is skipped as a whole.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.PARENT_GROUP)
                .label("Parent group")
                .helpText("Group under which the groups named by the entries are found or created. Required when the group field index is set.")
                .type(ProviderConfigProperty.GROUP_TYPE)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.ROLE_FIELD_INDEX)
                .label("Role field index")
                .helpText("Optional. Position of the role field in an entry, counting from 0. Its value is looked up in the role table.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .property()
                .name(ClaimToGroupsAndRolesConfig.ROLES)
                .label("Role table")
                .helpText("Role field value as key, and the role it grants as value: a realm role name, or 'clientId.roleName' for a client role. Exact match; a value missing from the table grants no role. Required when the role field index is set.")
                .type(ProviderConfigProperty.MAP_TYPE)
                .add()
            .build();

    private enum Sync {
        GRANT,
        GRANT_AND_REVOKE
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public Set<String> getGrantableRoleReferences(Map<String, String> config) {
        return ClaimToGroupsAndRolesConfig.roleReferences(config);
    }

    @Override
    public Map<String, String> withRewrittenRoleReferences(Map<String, String> config, UnaryOperator<String> rewriteReference) {
        return ClaimToGroupsAndRolesConfig.withRewrittenRoleReferences(config, rewriteReference);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Claim to Groups and Roles";
    }

    @Override
    public String getHelpText() {
        return "Split each entry of a claim on a delimiter, and map its fields to a subgroup of a parent group and to roles.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        map(session, realm, user, mapperModel, context, Sync.GRANT);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        map(session, realm, user, mapperModel, context, Sync.GRANT_AND_REVOKE);
    }

    private void map(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                     BrokeredIdentityContext context, Sync sync) {
        ClaimToGroupsAndRolesConfig.Parsed parsed = ClaimToGroupsAndRolesConfig.parse(mapperModel.getConfig());
        if (parsed instanceof Invalid invalid) {
            LOG.warnf("%s maps no groups or roles because its configuration is invalid: %s. Correct the mapper configuration.",
                    describe(mapperModel, realm), describeViolations(invalid));
        } else if (parsed instanceof Valid valid) {
            ClaimToGroupsAndRolesConfig config = valid.config();
            Mapping mapping = DelimitedClaimEntries.read(claimValue(context, config.claim(), mapperModel, realm), config);
            logSkipped(mapperModel, realm, mapping.skipped());
            config.groups().ifPresent(groups -> mapGroups(session, realm, user, mapperModel, context, sync, mapping.groupNames()));
            config.roles().ifPresent(roles -> mapRoles(session, realm, user, mapperModel, context, sync, roles, mapping.roles()));
        }
    }

    private static void logSkipped(IdentityProviderMapperModel mapperModel, RealmModel realm, List<Skipped> skipped) {
        if (LOG.isDebugEnabled()) {
            skipped.forEach(entry -> LOG.debugf("%s skipped claim entry %s because it %s.",
                    describe(mapperModel, realm), quoted(entry.entry()), entry.reason()));
        }
        refusedGroupNamesWarning(skipped).ifPresent(warning -> LOG.warnf("%s %s", describe(mapperModel, realm), warning));
    }

    private static String describeViolations(Invalid invalid) {
        return invalid.violations().stream().map(Object::toString).collect(Collectors.joining("; "));
    }

    /**
     * A summary of the entries skipped for a refused group name, naming the first {@value #MAX_LOGGED_ENTRIES}; empty
     * when there are none.
     */
    static Optional<String> refusedGroupNamesWarning(List<Skipped> skipped) {
        List<Skipped> refused = refusedGroupNameEntries(skipped);
        return refused.isEmpty() ? Optional.empty() : Optional.of(refusedGroupNamesSummary(refused));
    }

    private static List<Skipped> refusedGroupNameEntries(List<Skipped> skipped) {
        return skipped.stream().filter(entry -> entry.reason().refusedGroupName()).toList();
    }

    private static String refusedGroupNamesSummary(List<Skipped> refused) {
        return String.format(REFUSED_GROUP_NAMES_SUMMARY, refused.size(), firstLoggedEntries(refused),
                unloggedEntriesNote(refused.size()));
    }

    private static String firstLoggedEntries(List<Skipped> refused) {
        return refused.stream()
                .limit(MAX_LOGGED_ENTRIES)
                .map(ClaimToGroupsAndRolesMapper::describeSkipped)
                .collect(Collectors.joining("; "));
    }

    private static String describeSkipped(Skipped skipped) {
        return quoted(skipped.entry()) + " " + skipped.reason();
    }

    private static String unloggedEntriesNote(int entryCount) {
        return entryCount > MAX_LOGGED_ENTRIES ? " and " + (entryCount - MAX_LOGGED_ENTRIES) + " more" : "";
    }

    private static String describe(IdentityProviderMapperModel mapperModel, RealmModel realm) {
        return String.format("Mapper %s on identity provider %s in realm %s", quoted(mapperModel.getName()),
                quoted(mapperModel.getIdentityProviderAlias()), quoted(realm.getName()));
    }

    /**
     * The value in single quotes, safe to embed in one log line: cut to {@value #MAX_LOGGED_LENGTH} characters with
     * {@code ...} marking the cut, backslashes and quotes escaped, line breaks and other whitespace replaced by spaces,
     * and other control and format characters written as Java Unicode escapes.
     */
    static String quoted(String value) {
        String raw = Optional.ofNullable(value).orElse("");
        boolean cut = raw.codePointCount(0, raw.length()) > MAX_LOGGED_LENGTH;
        return "'" + escapeInvisible(escapeQuotesAndFlattenSpaces(loggedPrefix(raw))) + (cut ? "...'" : "'");
    }

    private static String loggedPrefix(String raw) {
        return raw.codePoints()
                .limit(MAX_LOGGED_LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static String escapeQuotesAndFlattenSpaces(String text) {
        return StringUtil.sanitizeSpacesAndQuotes(text.replace("\\", "\\\\"), '\'');
    }

    private static String escapeInvisible(String text) {
        return text.codePoints()
                .mapToObj(ClaimToGroupsAndRolesMapper::escapedCodePoint)
                .collect(Collectors.joining());
    }

    private static String escapedCodePoint(int codePoint) {
        return DelimitedClaimEntries.isInvisible(codePoint) ? String.format("\\u%04x", codePoint) : Character.toString(codePoint);
    }

    /**
     * Reads the claim from the access token, the ID token and the UserInfo response, in this order, and returns the
     * first value found.
     */
    private static Optional<JsonNode> claimValue(BrokeredIdentityContext context, String claim,
                                                 IdentityProviderMapperModel mapperModel, RealmModel realm) {
        return claimSets(context, mapperModel, realm)
                .flatMap(claims -> ClaimPath.find(claims, claim).stream())
                .findFirst();
    }

    private static Stream<JsonNode> claimSets(BrokeredIdentityContext context, IdentityProviderMapperModel mapperModel,
                                              RealmModel realm) {
        return Stream.of(OIDCIdentityProvider.VALIDATED_ACCESS_TOKEN, OIDCIdentityProvider.VALIDATED_ID_TOKEN, OIDCIdentityProvider.USER_INFO)
                .flatMap(source -> claimSet(source, context.getContextData().get(source), mapperModel, realm).stream());
    }

    private static Optional<JsonNode> claimSet(String source, Object contextData, IdentityProviderMapperModel mapperModel,
                                               RealmModel realm) {
        try {
            return readClaims(contextData);
        } catch (JWSInputException | IllegalArgumentException e) {
            LOG.debugf("%s cannot read the claims of %s, and looks for the claim in the other sources: %s",
                    describe(mapperModel, realm), source, quoted(e.getMessage()));
            return Optional.empty();
        }
    }

    private static Optional<JsonNode> readClaims(Object contextData) throws JWSInputException {
        if (contextData instanceof JsonNode claims) {
            return Optional.of(claims);
        }
        if (contextData instanceof JsonWebToken token) {
            return Optional.of(JsonSerialization.mapper.valueToTree(token));
        }
        if (contextData instanceof String encoded) {
            return Optional.of(new JWSInput(encoded).readJsonContent(JsonNode.class));
        }
        return Optional.empty();
    }

    private void mapGroups(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                           BrokeredIdentityContext context, Sync sync, Set<String> groupNames) {
        Optional.ofNullable(KeycloakModelUtils.getGroupForIdpMapper(session, realm, user, mapperModel, context))
                .map(group -> ParentGroup.of(session, realm, group))
                .ifPresent(parent -> syncGroups(session, realm, user, mapperModel, context, sync, parent, groupNames));
    }

    private record ClaimedSubgroup(String name, Optional<GroupModel> existing) {
    }

    /**
     * Joins the claimed subgroups the admin guard allows, creating the missing ones, and on revoking sync leaves the
     * other direct subgroups of the parent group.
     */
    private void syncGroups(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                            BrokeredIdentityContext context, Sync sync, ParentGroup parent, Set<String> groupNames) {
        List<ClaimedSubgroup> claimed = claimedSubgroups(parent, groupNames);
        if (sync == Sync.GRANT_AND_REVOKE) {
            leaveUnclaimedSubgroups(user, context, parent, existingIds(claimed));
        }
        List<GroupModel> existing = joinableExistingSubgroups(session, realm, mapperModel, claimed);
        List<GroupModel> created = createMissingSubgroups(session, realm, mapperModel, parent, missingNames(claimed));
        existing.forEach(group -> join(user, context, group));
        created.forEach(group -> join(user, context, group));
    }

    private static List<ClaimedSubgroup> claimedSubgroups(ParentGroup parent, Set<String> groupNames) {
        return groupNames.stream()
                .sorted()
                .map(name -> new ClaimedSubgroup(name, parent.findSubgroup(name)))
                .toList();
    }

    private static Stream<GroupModel> existingSubgroups(List<ClaimedSubgroup> claimed) {
        return claimed.stream().map(ClaimedSubgroup::existing).flatMap(Optional::stream);
    }

    private static Set<String> existingIds(List<ClaimedSubgroup> claimed) {
        return existingSubgroups(claimed).map(GroupModel::getId).collect(Collectors.toUnmodifiableSet());
    }

    private static List<String> missingNames(List<ClaimedSubgroup> claimed) {
        return claimed.stream()
                .filter(subgroup -> subgroup.existing().isEmpty())
                .map(ClaimedSubgroup::name)
                .toList();
    }

    /**
     * Leaves the direct subgroups of the parent group that the claim no longer names and no other mapper assigned in
     * this login.
     */
    private static void leaveUnclaimedSubgroups(UserModel user, BrokeredIdentityContext context, ParentGroup parent,
                                                Set<String> claimedIds) {
        List<GroupModel> unclaimed = parent.memberships(user)
                .filter(group -> parent.group().getId().equals(group.getParentId()))
                .filter(group -> !claimedIds.contains(group.getId()))
                .filter(group -> !context.hasMapperAssignedGroup(group.getId()))
                .toList();
        unclaimed.forEach(user::leaveGroup);
    }

    private static List<GroupModel> joinableExistingSubgroups(KeycloakSession session, RealmModel realm,
                                                              IdentityProviderMapperModel mapperModel, List<ClaimedSubgroup> claimed) {
        return existingSubgroups(claimed)
                .filter(group -> isAdminGroupJoinAllowed(session, realm, group, mapperModel))
                .toList();
    }

    /**
     * Creates the missing subgroups when the parent group passes the admin guard, since a new subgroup inherits the
     * parent's roles; creates none otherwise.
     */
    private static List<GroupModel> createMissingSubgroups(KeycloakSession session, RealmModel realm,
                                                           IdentityProviderMapperModel mapperModel, ParentGroup parent,
                                                           List<String> missing) {
        return !missing.isEmpty() && isAdminGroupJoinAllowed(session, realm, parent.group(), mapperModel)
                ? createSubgroups(parent, missing)
                : List.of();
    }

    private static List<GroupModel> createSubgroups(ParentGroup parent, List<String> names) {
        return names.stream().map(parent::createSubgroup).toList();
    }

    private static void join(UserModel user, BrokeredIdentityContext context, GroupModel group) {
        context.addMapperAssignedGroup(group.getId());
        user.joinGroup(group);
    }

    /**
     * The parent group of the mapped subgroups: a realm group, or a group of the organization the identity provider is
     * linked to.
     */
    private sealed interface ParentGroup permits RealmParentGroup, OrganizationParentGroup {

        GroupModel group();

        Optional<GroupModel> findSubgroup(String name);

        GroupModel createSubgroup(String name);

        /**
         * The groups of this group's kind the user is a direct member of.
         */
        Stream<GroupModel> memberships(UserModel user);

        static ParentGroup of(KeycloakSession session, RealmModel realm, GroupModel group) {
            return Organizations.isOrganizationGroup(group)
                    ? new OrganizationParentGroup(session.getProvider(OrganizationProvider.class), group.getOrganization(), group)
                    : new RealmParentGroup(session, realm, group);
        }
    }

    private record RealmParentGroup(KeycloakSession session, RealmModel realm, GroupModel group) implements ParentGroup {

        @Override
        public Optional<GroupModel> findSubgroup(String name) {
            return Optional.ofNullable(session.groups().getGroupByName(realm, group, name));
        }

        @Override
        public GroupModel createSubgroup(String name) {
            return session.groups().createGroup(realm, name, group);
        }

        @Override
        public Stream<GroupModel> memberships(UserModel user) {
            return user.getGroupsStream();
        }
    }

    private record OrganizationParentGroup(OrganizationProvider organizations, OrganizationModel organization,
                                           GroupModel group) implements ParentGroup {

        @Override
        public Optional<GroupModel> findSubgroup(String name) {
            return organizations.searchGroupsByName(organization, name, true, null, null)
                    .filter(candidate -> group.getId().equals(candidate.getParentId()))
                    .findFirst();
        }

        @Override
        public GroupModel createSubgroup(String name) {
            return organizations.createGroup(organization, name, group);
        }

        @Override
        public Stream<GroupModel> memberships(UserModel user) {
            return organizations.getOrganizationGroupsByMember(organization, user);
        }
    }

    private record GrantedRole(String reference, RoleModel role) {
    }

    private void mapRoles(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                          BrokeredIdentityContext context, Sync sync, RoleOutput output, Set<String> roleReferences) {
        if (sync == Sync.GRANT_AND_REVOKE) {
            revokeUnclaimedRoles(session, realm, user, context, output, roleReferences);
        }
        grantableRoles(session, realm, mapperModel, roleReferences).forEach(role -> grant(user, context, role));
    }

    private static List<GrantedRole> grantableRoles(KeycloakSession session, RealmModel realm,
                                                    IdentityProviderMapperModel mapperModel, Set<String> roleReferences) {
        return roleReferences.stream()
                .sorted()
                .flatMap(reference -> grantableRole(session, realm, mapperModel, reference).stream())
                .toList();
    }

    /**
     * Revokes the roles of the role table that the claim no longer grants and no other mapper granted in this login.
     */
    private static void revokeUnclaimedRoles(KeycloakSession session, RealmModel realm, UserModel user,
                                             BrokeredIdentityContext context, RoleOutput output, Set<String> roleReferences) {
        output.ownedRoles().stream()
                .filter(reference -> !roleReferences.contains(reference))
                .filter(reference -> !context.hasMapperGrantedRole(reference))
                .flatMap(reference -> existingRole(session, realm, reference).stream())
                .filter(user::hasDirectRole)
                .forEach(user::deleteRoleMapping);
    }

    private static Optional<GrantedRole> grantableRole(KeycloakSession session, RealmModel realm,
                                                IdentityProviderMapperModel mapperModel, String reference) {
        return existingRoleOrWarn(session, realm, mapperModel, reference)
                .filter(role -> isAdminRoleGrantAllowed(session, realm, role, mapperModel))
                .map(role -> new GrantedRole(reference, role));
    }

    private static void grant(UserModel user, BrokeredIdentityContext context, GrantedRole granted) {
        context.addMapperGrantedRole(granted.reference());
        user.grantRole(granted.role());
    }

    private static Optional<RoleModel> existingRole(KeycloakSession session, RealmModel realm, String reference) {
        return Optional.ofNullable(KeycloakModelUtils.getRoleFromString(session, realm, reference));
    }

    private static Optional<RoleModel> existingRoleOrWarn(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, String reference) {
        Optional<RoleModel> role = existingRole(session, realm, reference);
        if (role.isEmpty()) {
            LOG.warnf("%s cannot grant role %s because the realm has no such role. Create the role or correct the role table.",
                    describe(mapperModel, realm), quoted(reference));
        }
        return role;
    }
}
