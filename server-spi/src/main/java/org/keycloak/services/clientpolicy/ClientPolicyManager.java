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
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Provides a method for handling an event defined in {@link ClientPolicyEvent}.
 * Also provides methods for handling client profiles and policies.
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientPolicyManager {

    /**
     * execute a method for handling an event defined in {@link ClientPolicyEvent}.
     * 
     * @param context - the context of the event.
     * @throws {@link ClientPolicyException}
     */
    void triggerOnEvent(ClientPolicyContext context) throws ClientPolicyException;

    /**
     * when booting keycloak, reads json representations of the builtin client profiles and policies from files 
     * enclosed in keycloak-services jar file and put them onto the keycloak application.
     * if these operation fails, put null.
     * 
     * @param profilesFilePath - the file path for the builtin client profiles
     * @param policiesFilePath - the file path for the builtin client policies
     */
    void setupClientPoliciesOnKeycloakApp(String profilesFilePath, String policiesFilePath);

    /**
     * when creating a realm, reads the builtin client profiles and policies
     * that have already been set on keycloak application on booting keycloak and put them onto the realm as its attribute.
     * if these operation fails, put null.
     * 
     * @param realm - the newly created realm
     */
    void setupClientPoliciesOnCreatedRealm(RealmModel realm);

    /**
     * when importing a realm, reads the builtin client profiles and policies
     * that have already been set on keycloak application on booting keycloak and override them
     * with ones loaded from the imported realm json file.
     * if these operation fails, rolls them back to the builtin client profiles and policies set on keycloak application. 
     * 
     * @param realm - the newly created realm to be overriden by imported realm's representation
     * @param rep - imported realm's representation
     */
    void setupClientPoliciesOnImportedRealm(RealmModel realm, RealmRepresentation rep);

    /**
     * when updating client profiles via Admin REST API, reads the json representation of the client profiles
     * and overrides the existing client profiles set on the realm with them.
     * if these operation fails, rolls them back to the existing client profiles and throw an exception.
     *
     * If the "clientProfiles" parameter contains the builtin client profiles, it is checked that they do NOT update the existing built-in client profiles. Also
     * it is not possible to create new built-in client profiles or remove built-in client profiles. If some existing realm built-in client profiles are NOT present
     * in the "clientProfiles" parameter, they are just kept in the realm, but not removed.
     * 
     * @param realm - the realm whose client profiles is to be overriden by the new client profiles
     * @param clientProfiles - the json representation of the new client profiles that overrides the existing client profiles set on the realm. With
     *                       the exception of built-in profiles, which are not overriden as mentioned above.
     * @throws {@link ClientPolicyException}
     */
    void updateClientProfiles(RealmModel realm, ClientProfilesRepresentation clientProfiles) throws ClientPolicyException;

    /**
     * when getting client profiles via Admin REST API, returns the existing client profiles set on the realm.
     * 
     * @param realm - the realm whose client profiles is to be returned
     * @return the json representation of the client profiles set on the realm
     */
    ClientProfilesRepresentation getClientProfiles(RealmModel realm) throws ClientPolicyException;

    /**
     * when updating client policies via Admin REST API, reads the json representation of the client policies
     * and overrides the existing client policies set on the realm with them.
     * if these operation fails, rolls them back to the existing client policies and throw an exception.
     *
     * If the "clientProfiles" parameter contains the builtin client policies, it is checked that they do NOT update the existing built-in client profiles.
     * The only thing, which is allowed to update in the built-in policies, is the "enabled" status, so that built-in policies can be disabled.
     * Also it is not possible to create new built-in client profiles or remove built-in client profiles. If some existing realm built-in client profiles are NOT present
     * in the "clientProfiles" parameter, they are just kept in the realm, but not removed.
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
     * when exporting realm the realm, prepares the exported representation of the client profiles and policies.
     * E.g. the builtin client profiles and policies are filtered out and not exported. 
     * 
     * @param realm - the realm to be exported
     * @param rep - the realm's representation to be exported actually
     */
    void setupClientPoliciesOnExportingRealm(RealmModel realm, RealmRepresentation rep);

    /**
     * returns the json representation of the builtin client profiles set on keycloak application.
     * 
     * @return the json representation of the builtin client profiles set on keycloak application
     */
    String getClientProfilesOnKeycloakApp();

    /**
     * returns the json representation of the builtin client policies set on keycloak application.
     * 
     * @return the json representation of the builtin client policies set on keycloak application
     */
    String getClientPoliciesOnKeycloakApp();

    /**
     * returns the json representation of the client profiles set on the realm.
     * 
     * @param realm - the realm whose client profiles is to be returned
     * @return the json representation of the client profiles set on the realm
     */
    String getClientProfilesJsonString(RealmModel realm);

    /**
     * returns the json representation of the client policies set on the realm.
     * 
     * @param realm - the realm whose client policies is to be returned
     * @return the json representation of the client policies set on the realm
     */
    String getClientPoliciesJsonString(RealmModel realm);

}
