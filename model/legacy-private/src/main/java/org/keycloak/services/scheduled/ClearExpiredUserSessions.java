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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredUserSessions implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(ClearExpiredUserSessions.class);

    public static final String TASK_NAME = "ClearExpiredUserSessions";

    @Override
    public void run(KeycloakSession session) {
        long currentTimeMillis = Time.currentTimeMillis();

        session.authenticationSessions().removeAllExpired();
        session.sessions().removeAllExpired();

        long took = Time.currentTimeMillis() - currentTimeMillis;
        logger.debugf("ClearExpiredUserSessions finished in %d ms", took);
    }

}
