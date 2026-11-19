/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.Token;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.ScimGroupOwnerEntity;
import org.keycloak.representations.AccessToken;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.resource.spi.MembershipChange;
import org.keycloak.storage.UserStorageUtil;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * Groups owned by the SCIM client that created them.
 *
 * <p>A realm turns this on with two switches: the experimental server feature {@code scim-group-membership-claims},
 * and the realm attribute {@value #REALM_ATTRIBUTE} set to {@code true}. With either one off, nothing here runs and
 * SCIM behaves as it does without the feature.
 *
 * <p>When it is on, the SCIM client that creates a group with {@code POST /Groups} owns it. The owner is stored by the
 * client's {@code clientId}. Only the owner may change or delete the group through SCIM, and the owner may send the
 * full member list with {@code PUT}, or with a PATCH that replaces {@code members}. Members that the regular PATCH
 * refuses to manage, such as service accounts and administrators, are never removed by an owner's write: a full list
 * that leaves one out keeps it. A group that no SCIM client created behaves as it does without the feature, and never
 * gets an owner later.
 *
 * <p>Every change to an owned group's members first locks the group row, so that two requests for the same group run
 * one after the other.
 */
public final class GroupOwnership {

    public static final String REALM_ATTRIBUTE = "scimGroupMembershipClaims";

    static final String MANAGED_BY_ANOTHER_CLIENT = "The group is managed by another SCIM client";

    private final KeycloakSession session;
    private final GroupCoreModelSchema schema;
    private final EntityManager em;

    GroupOwnership(KeycloakSession session, GroupCoreModelSchema schema) {
        this.session = session;
        this.schema = schema;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public static boolean isEnabled(RealmModel realm) {
        return Profile.isFeatureEnabled(Profile.Feature.SCIM_GROUP_MEMBERSHIP_CLAIMS)
                && Boolean.parseBoolean(realm.getAttribute(REALM_ATTRIBUTE));
    }

    /**
     * The ids, among {@code groupIds}, of the groups that a SCIM client owns.
     */
    public static Set<String> ownedAmong(KeycloakSession session, Collection<String> groupIds) {
        if (groupIds.isEmpty()) {
            return Set.of();
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<String> owned = em.createQuery("select o.groupId from ScimGroupOwnerEntity o where o.groupId in :groupIds", String.class)
                .setParameter("groupIds", groupIds)
                .getResultList();

        return new HashSet<>(owned);
    }

    /**
     * The {@code clientId} of the client that owns {@code group}, or {@code null} when no SCIM client owns it.
     */
    String ownerOf(GroupModel group) {
        ScimGroupOwnerEntity entity = em.find(ScimGroupOwnerEntity.class, group.getId());
        return entity == null ? null : entity.getClientId();
    }

    /**
     * Records {@code owner} as the client that created {@code group}.
     */
    void recordOwner(GroupModel group, String owner) {
        em.persist(new ScimGroupOwnerEntity(group.getId(), owner));
        em.flush();
    }

    /**
     * The {@code clientId} of the client the access token of the current request was issued to, or {@code null} when
     * the token names no client of the realm.
     */
    String caller() {
        Token token = session.getContext().getBearerToken();

        if (!(token instanceof AccessToken accessToken) || isBlank(accessToken.getIssuedFor())) {
            return null;
        }

        ClientModel client = session.getContext().getRealm().getClientByClientId(accessToken.getIssuedFor());

        return client == null ? null : client.getClientId();
    }

    /**
     * The {@code clientId} of the calling client. It becomes the owner of a group it creates.
     */
    String requireCaller() {
        String caller = caller();

        if (caller == null) {
            throw new ForbiddenException();
        }

        return caller;
    }

    /**
     * Refuses the request unless the caller is {@code owner}.
     */
    void checkOwner(String owner) {
        if (!owner.equals(caller())) {
            throw new ForbiddenException(MANAGED_BY_ANOTHER_CLIENT);
        }
    }

    /**
     * Refuses the request when another SCIM client owns {@code group} or any group below it, since deleting a group
     * deletes its subgroups too. A group that no SCIM client owns is not checked.
     */
    void checkDelete(GroupModel group) {
        String owner = ownerOf(group);

        if (owner != null) {
            checkOwner(owner);
        }

        group.getSubGroupsStream().forEach(this::checkDelete);
    }

    /**
     * Checks that the caller may change the members of {@code group}, as the regular PATCH does.
     */
    void checkGroup(GroupModel group) {
        GroupCoreModelSchema.checkGroupMembershipPermission(session.getContext().getPermissions(), group);
    }

    /**
     * Locks the group row until the transaction ends. Reads of the members made after this see every change committed
     * before it.
     */
    void lock(GroupModel group) {
        em.flush();
        GroupEntity entity = em.find(GroupEntity.class, group.getId(), LockModeType.PESSIMISTIC_WRITE);

        if (entity == null) {
            throw new ModelValidationException("Invalid group");
        }
    }

    /**
     * The ids of the direct members of {@code group}, wherever they are stored. Call {@link #lock(GroupModel)} first.
     */
    Set<String> currentMembers(GroupModel group) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().getGroupMembersStream(realm, group)
                .map(UserModel::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Checks a member that a PATCH {@code add} or {@code remove} names, the way the regular PATCH checks it: the user
     * must exist, and the caller must be allowed to change the user's memberships, whether or not the user is a
     * member.
     *
     * @return the user id
     */
    String named(String userId) {
        checkMayChange(find(userId));
        return userId;
    }

    /**
     * Checks a member of a full member list: a {@code POST}, a {@code PUT}, or a PATCH that replaces {@code members}.
     * The user must exist. A user who is already a member is accepted as it is, even one the caller may not change, so
     * that a list read back from the group can be sent again. Any other user is checked as {@link #named(String)}
     * checks it.
     *
     * @return the user id
     */
    String listed(String userId, Set<String> current) {
        UserModel user = find(userId);

        if (!current.contains(userId)) {
            checkMayChange(user);
        }

        return userId;
    }

    /**
     * Works out how to get from the {@code current} members to the {@code desired} ones, and checks the change before
     * anything is changed. A current member that the caller may not change, for example a service account or an
     * administrator, stays even when {@code desired} leaves it out. Every user in {@code desired} that is not a current
     * member must have been checked with {@link #named(String)} or {@link #listed(String, Set)}.
     */
    Change plan(GroupModel group, Set<String> current, Set<String> desired) {
        RealmModel realm = session.getContext().getRealm();
        Set<String> stored = storedMembers(group);
        List<String> leave = new ArrayList<>();
        List<String> join = new ArrayList<>();

        for (String userId : current) {
            if (desired.contains(userId)) {
                continue;
            }

            UserModel user = session.users().getUserById(realm, userId);

            if (user == null || !mayChange(user)) {
                // protected: kept, whatever the list says
                continue;
            }

            if (!stored.contains(userId) && !isStored(group.getId(), userId)) {
                // An administrator does not take the group lock, so the membership may have been removed after the
                // members were read. Only a membership the user still has outside the database is refused.
                UserModel fresh = uncached(realm, userId);

                if (fresh != null && fresh.getGroupsStream().anyMatch(g -> g.getId().equals(group.getId()))) {
                    throw cannotManage(userId);
                }

                continue;
            }

            leave.add(userId);
        }

        for (String userId : desired) {
            if (!current.contains(userId)) {
                join.add(userId);
            }
        }

        return new Change(leave, join);
    }

    /**
     * Applies a change worked out by {@link #plan(GroupModel, Set, Set)}, reporting each membership that is added or
     * removed.
     */
    void apply(GroupModel group, Change change, Consumer<MembershipChange> changes) {
        RealmModel realm = session.getContext().getRealm();

        for (String userId : change.leave()) {
            UserModel user = uncached(realm, userId);

            if (user != null && isStored(group.getId(), userId)) {
                user.leaveGroup(group);
                changes.accept(new MembershipChange(group, user, false));
            }
        }

        for (String userId : change.join()) {
            UserModel user = uncached(realm, userId);

            if (user == null) {
                throw new ModelValidationException("User with id " + userId + " not found");
            }

            if (isStored(group.getId(), userId)) {
                continue;
            }

            user.joinGroup(group);
            em.flush();

            if (!isStored(group.getId(), userId)) {
                // The membership is stored outside the Keycloak database, for example by an LDAP group mapper.
                throw cannotManage(userId);
            }

            changes.accept(new MembershipChange(group, user, true));
        }

        em.flush();
    }

    /**
     * The members to remove and to add.
     */
    record Change(List<String> leave, List<String> join) {
    }

    private UserModel find(String userId) {
        if (isBlank(userId)) {
            throw new ModelValidationException("Member value is required");
        }

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);

        if (user == null) {
            throw new ModelValidationException("User with id " + userId + " not found");
        }

        return user;
    }

    private void checkMayChange(UserModel user) {
        if (!schema.canViewUser(user)) {
            throw new ModelValidationException("User with id " + user.getId() + " not found");
        }

        schema.checkRequireManageGroupMembership(session.getContext().getPermissions(), user);
    }

    private boolean mayChange(UserModel user) {
        try {
            checkMayChange(user);
            return true;
        } catch (ForbiddenException | ModelValidationException e) {
            return false;
        }
    }

    private static ModelValidationException cannotManage(String userId) {
        return new ModelValidationException("Membership of user " + userId + " cannot be managed through SCIM");
    }

    /**
     * The user as stored in the database. A cached user can still list a membership that a request committed while
     * this one waited for the group lock, and then {@code joinGroup} and {@code leaveGroup} change nothing. Evicting
     * the user makes both read the database, and drops the stale entry from the cache when the transaction commits.
     */
    private UserModel uncached(RealmModel realm, String userId) {
        UserCache cache = UserStorageUtil.userCache(session);
        UserModel user = session.users().getUserById(realm, userId);

        if (cache != null && user != null) {
            cache.evict(realm, user);
            user = session.users().getUserById(realm, userId);
        }

        return user;
    }

    /**
     * The ids of the members of {@code group} whose membership is stored in the Keycloak database.
     */
    private Set<String> storedMembers(GroupModel group) {
        List<String> ids = em.createQuery("select m.user.id from UserGroupMembershipEntity m where m.groupId = :groupId", String.class)
                .setParameter("groupId", group.getId())
                .getResultList();
        return new HashSet<>(ids);
    }

    private boolean isStored(String groupId, String userId) {
        return em.createQuery("select count(m) from UserGroupMembershipEntity m where m.groupId = :groupId and m.user.id = :userId", Long.class)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .getSingleResult() > 0;
    }
}
