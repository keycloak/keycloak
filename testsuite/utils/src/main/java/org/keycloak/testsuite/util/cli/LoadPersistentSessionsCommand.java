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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

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
        final int workersCount = getIntArg(0);
        final int limit = getIntArg(1);
        //int workersCount = 8;
        //int limit = 64;

        AtomicReference<String> lastSessionId = new AtomicReference<>("abc");

        AtomicBoolean finished = new AtomicBoolean(false);
        int i=0;

        while (!finished.get()) {
            if (i % 16 == 0) {
                log.infof("Starting iteration: %s . lastCreatedOn: %d, lastSessionId: %s", i, lastSessionId.get());
            }

            i = i + workersCount;
            List<Thread> workers = new LinkedList<>();
            MyWorker lastWorker = null;

            for (int workerId = 0 ; workerId < workersCount ; workerId++) {
                lastWorker = new MyWorker(workerId, lastSessionId.get(), limit, sessionFactory);
                Thread worker = new Thread(lastWorker);
                workers.add(worker);
            }

            for (Thread worker : workers) {
                worker.start();
            }
            for (Thread worker : workers) {
                try {
                    worker.join();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            List<UserSessionModel> lastWorkerSessions = lastWorker.getLoadedSessions();

            if (lastWorkerSessions.size() < limit) {
                finished.set(true);
            } else {
                UserSessionModel lastSession = lastWorkerSessions.get(lastWorkerSessions.size() - 1);
                lastSessionId.set(lastSession.getId());
            }


        }

        log.info("All persistent sessions loaded successfully");
    }

    @Override
    public String printUsage() {
        return super.printUsage() + " <workers-count (for example 8)> <limit (for example 64)>";
    }


    private static class MyWorker implements Runnable {

        private final int workerId;
        private final String lastSessionId;
        private final int limit;
        private final KeycloakSessionFactory sessionFactory;

        private List<UserSessionModel> loadedSessions = new LinkedList<>();

        public MyWorker(int workerId, String lastSessionId, int limit, KeycloakSessionFactory sessionFactory) {
            this.workerId = workerId;
            this.lastSessionId = lastSessionId;
            this.limit = limit;
            this.sessionFactory = sessionFactory;
        }

        @Override
        public void run() {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, (keycloakSession) -> {
                int offset = workerId * limit;

                UserSessionPersisterProvider persister = keycloakSession.getProvider(UserSessionPersisterProvider.class);
                loadedSessions = persister
                        .loadUserSessionsStream(offset, limit, true, lastSessionId)
                        .collect(Collectors.toList());

            });
        }


        private List<UserSessionModel> getLoadedSessions() {
            return loadedSessions;
        }
    }
}
