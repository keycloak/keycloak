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

package org.keycloak.sessions;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationSessionProvider extends Provider {

    /**
     * Creates and registers a new authentication session with random ID. Authentication session
     * entity will be prefilled with current timestamp, the given realm and client.
     */
    RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm);

    RootAuthenticationSessionModel createRootAuthenticationSession(String id, RealmModel realm);

    RootAuthenticationSessionModel getRootAuthenticationSession(RealmModel realm, String authenticationSessionId);

    void removeRootAuthenticationSession(RealmModel realm, RootAuthenticationSessionModel authenticationSession);

    void removeExpired(RealmModel realm);
    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);

    /**
     * Requests update of authNotes of a root authentication session that is not owned
     * by this instance but might exist somewhere in the cluster.
     * 
     * @param compoundId
     * @param authNotesFragment Map with authNote values. Auth note is removed if the corresponding value in the map is {@code null}.
     */
    void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment);


}
