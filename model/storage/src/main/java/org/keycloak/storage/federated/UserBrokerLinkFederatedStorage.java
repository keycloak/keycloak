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

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserBrokerLinkFederatedStorage {
    String getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm);
    void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel socialLink);
    boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider);
    void preRemove(RealmModel realm, IdentityProviderModel provider);
    void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel federatedIdentityModel);

    /**
     * Obtains the identities of the federated user identified by {@code userId}.
     *
     * @param userId the user identifier.
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of federated identities associated with the user.
     */
    Stream<FederatedIdentityModel> getFederatedIdentitiesStream(String userId, RealmModel realm);

    FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm);

    /**
     * @deprecated This interface is no longer necessary; collection-based methods were removed from the parent interface
     * and therefore the parent interface can be used directly
     */
    @Deprecated
    interface Streams extends UserBrokerLinkFederatedStorage {
    }
}
