/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.managers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.services.util.UserSessionUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 *
 * @deprecated To be removed without replacement. Check the methods documentation for alternatives.
 */
@Deprecated(since = "26", forRemoval = true)
public class UserSessionCrossDCManager {

    private final KeycloakSession kcSession;

    public UserSessionCrossDCManager(KeycloakSession session) {
        this.kcSession = session;
    }


    // get userSession if it has "authenticatedClientSession" of specified client attached to it. Otherwise download it from remoteCache

    /**
     * @deprecated To be removed in Keycloak 27+. Use
     * {@link UserSessionProvider#getUserSessionIfClientExists(RealmModel, String, boolean, String)}
     */
    @Deprecated(since = "26", forRemoval = true)
    public UserSessionModel getUserSessionWithClient(RealmModel realm, String id, boolean offline, String clientUUID) {
        return kcSession.sessions().getUserSessionIfClientExists(realm, id, offline, clientUUID);
    }

    /**
     * @deprecated To be removed in Keycloak 27+. Use
     * {@link UserSessionUtil#getUserSessionWithImpersonatorClient(KeycloakSession, RealmModel, String, boolean, String)}
     */
    @Deprecated(since = "26", forRemoval = true)
    public UserSessionModel getUserSessionWithImpersonatorClient(RealmModel realm, String id, boolean offline, String clientUUID) {
        return UserSessionUtil.getUserSessionWithImpersonatorClient(kcSession, realm, id, offline, clientUUID);
    }

    // get userSession if it has "authenticatedClientSession" of specified client attached to it. Otherwise download it from remoteCache
    // TODO Probably remove this method once AuthenticatedClientSession.getAction is removed and information is moved to OAuth code JWT instead
    /**
     * @deprecated To be removed in Keycloak 27+. Use
     * {@link UserSessionProvider#getUserSessionIfClientExists(RealmModel, String, boolean, String)}
     */
    @Deprecated(since = "26", forRemoval = true)
    public UserSessionModel getUserSessionWithClient(RealmModel realm, String id, String clientUUID) {
        return getUserSessionWithClient(realm, id, false, clientUUID);
    }

    // Just check if userSession also exists on remoteCache. It can happen that logout happened on 2nd DC and userSession is already removed on remoteCache and this DC wasn't yet notified
    /**
     * @deprecated To be removed in Keycloak 27+. Use
     * {@link AuthenticationSessionManager#getUserSessionFromAuthenticationCookie(RealmModel)}
     */
    @Deprecated(since = "26", forRemoval = true)
    public UserSessionModel getUserSessionIfExistsRemotely(AuthenticationSessionManager asm, RealmModel realm) {
        return asm.getUserSessionFromAuthenticationCookie(realm);
    }
}
