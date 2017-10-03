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

package org.keycloak.testsuite.util.cli;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoadPersistentSessionsCommand extends AbstractCommand {

    @Override
    public String getName() {
        return "loadPersistentSessions";
    }

    @Override
    protected void doRunCommand(KeycloakSession session) {
        int sessionsPerSegment = getIntArg(0);
        UserSessionProviderFactory sessionProviderFactory = (UserSessionProviderFactory) sessionFactory.getProviderFactory(UserSessionProvider.class);
        sessionProviderFactory.loadPersistentSessions(sessionFactory, 10, sessionsPerSegment);

        log.info("All persistent sessions loaded successfully");
    }

    @Override
    public String printUsage() {
        return super.printUsage() + " <sessions-count-per-segment>";
    }
}
