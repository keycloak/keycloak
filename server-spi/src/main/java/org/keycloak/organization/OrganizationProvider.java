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
     * Returns the organizations of the given realm as a stream.
     * @param realm Realm.
     * @return Stream of the organizations. Never returns {@code null}.
     */
    Stream<OrganizationModel> getOrganizationsStream(RealmModel realm);

}
