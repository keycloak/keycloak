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

package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.timer.ScheduledTask;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredUserSessions implements ScheduledTask {

    public static final String TASK_NAME = "ClearExpiredUserSessions";

    @Override
    public void run(KeycloakSession session) {
        UserSessionProvider sessions = session.sessions();
        for (RealmModel realm : session.realms().getRealms()) {
            sessions.removeExpired(realm);
            session.authenticationSessions().removeExpired(realm);
            session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
        }
    }

}
