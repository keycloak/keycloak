/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.federated;

import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederatedStorageProvider extends Provider,
        UserAttributeFederatedStorage,
        UserBrokerLinkFederatedStorage,
        UserConsentFederatedStorage,
        UserNotBeforeFederatedStorage,
        UserGroupMembershipFederatedStorage,
        UserRequiredActionsFederatedStorage,
        UserRoleMappingsFederatedStorage,
        UserFederatedUserCredentialStore {

    /**
     * Obtains the ids of all federated users in the realm.
     *
     * @param realm a reference to the realm.
     * @param first first result to return. Ignored if negative or {@code null}.
     * @param max maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of federated user ids.
     */
    Stream<String> getStoredUsersStream(RealmModel realm, Integer first, Integer max);

    int getStoredUsersCount(RealmModel realm);

    void preRemove(RealmModel realm);

    void preRemove(RealmModel realm, GroupModel group);

    void preRemove(RealmModel realm, RoleModel role);

    void preRemove(RealmModel realm, ClientModel client);

    void preRemove(ProtocolMapperModel protocolMapper);

    void preRemove(ClientScopeModel clientScope);

    void preRemove(RealmModel realm, UserModel user);

    void preRemove(RealmModel realm, ComponentModel model);

    /**
     * @deprecated This interface is no longer necessary; collection-based methods were removed from the parent interface
     * and therefore the parent interface can be used directly
     */
    @Deprecated
    interface Streams extends UserFederatedStorageProvider,
            UserAttributeFederatedStorage.Streams,
            UserBrokerLinkFederatedStorage.Streams,
            UserConsentFederatedStorage.Streams,
            UserFederatedUserCredentialStore.Streams,
            UserGroupMembershipFederatedStorage.Streams,
            UserRequiredActionsFederatedStorage.Streams,
            UserRoleMappingsFederatedStorage.Streams {
    }
}
