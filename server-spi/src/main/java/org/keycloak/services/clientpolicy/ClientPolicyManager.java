/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Provides a method for handling an event defined in {@link ClientPolicyEvent}.
 * Also provides methods for handling client profiles and policies.
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientPolicyManager extends Provider {

    /**
     * execute a method for handling an event defined in {@link ClientPolicyEvent}.
     * 
     * @param context - the context of the event.
     * @throws {@link ClientPolicyException}
     */
    void triggerOnEvent(ClientPolicyContext context) throws ClientPolicyException;

    /**
     * when creating a realm, adds the default client policies, which should be available on the realm and put them onto the realm as its attribute.
     * if these operation fails, put null.
     *
     * @param realm - the newly created realm
     */
    void setupClientPoliciesOnCreatedRealm(RealmModel realm);

    /**
     * when importing a realm, or updating a realm, update model from the representation object
     *
     * @param realm - the newly created realm to be overriden by imported realm's representation
     * @param rep - imported realm's representation
     */
    void updateRealmModelFromRepresentation(RealmModel realm, RealmRepresentation rep);

    /**
     * when updating client profiles via Admin REST API, reads the json representation of the client profiles
     * and overrides the existing client profiles set on the realm with them.
     * if these operation fails, rolls them back to the existing client profiles and throw an exception.
     *
     * If the "clientProfiles" parameter contains the global client profiles, they won't be updated on the realm at all
     * 
     * @param realm - the realm whose client profiles is to be overriden by the new client profiles
     * @param clientProfiles - the json representation of the new client profiles that overrides the existing client profiles set on the realm. With
     *                       the exception of global profiles, which are not overriden as mentioned above.
     * @throws {@link ClientPolicyException}
     */
    void updateClientProfiles(RealmModel realm, ClientProfilesRepresentation clientProfiles) throws ClientPolicyException;

    /**
     * when getting client profiles via Admin REST API, returns the existing client profiles set on the realm.
     * 
     * @param realm - the realm whose client profiles is to be returned
     * @param includeGlobalProfiles - If true, method will return realm profiles and global profiles as well. If false, then "globalProfiles" field would be null
     * @return the json representation of the client profiles set on the realm
     */
    ClientProfilesRepresentation getClientProfiles(RealmModel realm, boolean includeGlobalProfiles) throws ClientPolicyException;

    /**
     * when updating client policies via Admin REST API, reads the json representation of the client policies
     * and overrides the existing client policies set on the realm with them.
     * if these operation fails, rolls them back to the existing client policies and throw an exception.
     *
     * @param realm - the realm whose client policies is to be overriden by the new client policies
     * @param clientPolicies - the json representation of the new client policies that overrides the existing client policies set on the realm
     * @throws {@link ClientPolicyException}
     */
    void updateClientPolicies(RealmModel realm, ClientPoliciesRepresentation clientPolicies) throws ClientPolicyException;

    /**
     * when getting client policies via Admin REST API, returns the existing client policies set on the realm.
     * 
     * @param realm - the realm whose client policies is to be returned
     * @return the json representation of the client policies set on the realm
     */
    ClientPoliciesRepresentation getClientPolicies(RealmModel realm) throws ClientPolicyException;

    /**
     * when exporting realm, or retrieve the realm for admin REST API, prepares the exported representation of the client profiles and policies.
     * Global client profiles and policies are filtered out and not exported.
     *
     * @param realm - the realm to be exported
     * @param rep - the realm's representation to be exported actually
     */
    void updateRealmRepresentationFromModel(RealmModel realm, RealmRepresentation rep);

}
