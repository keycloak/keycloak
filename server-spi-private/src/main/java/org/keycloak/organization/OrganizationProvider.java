/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.keycloak.organization;

import java.util.stream.Stream;

import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface OrganizationProvider extends Provider {

    /**
     * Creates a new organization with given {@code name} to the given realm.
     * The internal ID of the organization will be created automatically.
     * @param realm Realm owning this organization.
     * @param name String name of the organization.
     * @throws ModelDuplicateException If there is already an organization with the given name
     * @return Model of the created organization.
     */
    OrganizationModel createOrganization(RealmModel realm, String name);

    /**
     * Returns a {@link OrganizationModel} by its {@code id};
     *
     * @param realm the realm
     * @param id the id of an organization
     * @return the organization with the given {@code id}
     */
    OrganizationModel getOrganizationById(RealmModel realm, String id);

    /**
     * Removes the given organization from the given realm.
     *
     * @param realm Realm.
     * @param organization Organization to be removed.
     * @return true if the organization was removed, false if group doesn't exist or doesn't belong to the given realm
     */
    boolean removeOrganization(RealmModel realm, OrganizationModel organization);

    /**
     * Removes all organizations from the given realm.
     * @param realm Realm.
     */
    void removeOrganizations(RealmModel realm);

    /**
     * Adds the give {@link UserModel} as a member of the given {@link OrganizationModel}.
     *
     * @param realm the realm
     * @param organization the organization
     * @param user the user
     * @return {@code true} if the user was added as a member. Otherwise, returns {@code false}
     */
    boolean addOrganizationMember(RealmModel realm, OrganizationModel organization, UserModel user);

    /**
     * Returns the organizations of the given realm as a stream.
     * @param realm Realm.
     * @return Stream of the organizations. Never returns {@code null}.
     */
    Stream<OrganizationModel> getOrganizationsStream(RealmModel realm);

    /**
     * Returns the members of a given {@code organization}.
     *
     * @param realm the realm
     * @param organization the organization
     * @return the organization with the given {@code id}
     */
    Stream<UserModel> getMembersStream(RealmModel realm, OrganizationModel organization);

    /**
     * Returns the member of an {@code organization} by its {@code id}.
     *
     * @param realm the realm
     * @param organization the organization
     * @param id the member id
     * @return the organization with the given {@code id}
     */
    UserModel getMemberById(RealmModel realm, OrganizationModel organization, String id);

    /**
     * Returns the {@link OrganizationModel} that a {@code member} belongs to.
     *
     * @param realm the realm
     * @param member the member of a organization
     * @return the organization the {@code member} belongs to
     */
    OrganizationModel getOrganizationByMember(RealmModel realm, UserModel member);
}
