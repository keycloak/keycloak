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

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClusterProviderTaskCommand extends AbstractCommand {

    private static final ExecutorService executors = Executors.newCachedThreadPool();

    @Override
    protected void doRunCommand(KeycloakSession session) {
        String taskName = getArg(0);
        int taskTimeout = getIntArg(1);
        int sleepTime = getIntArg(2);

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        Future future = cluster.executeIfNotExecutedAsync(taskName, taskTimeout, () -> {
            log.infof("Started sleeping for " + sleepTime + " seconds");
            Thread.sleep(sleepTime * 1000);
            log.infof("Stopped sleeping");
            return null;
        });

        log.info("I've retrieved future successfully");

        executors.execute(() -> {
            try {
                future.get();
                log.info("Successfully finished future!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateConfig(MultivaluedHashMap<String, String> cfg, int waitTime) {
        cfg.putSingle("wait-time", String.valueOf(waitTime));
    }


    @Override
    public String getName() {
        return "clusterProviderTask";
    }

    @Override
    public String printUsage() {
        return super.printUsage() + " <task-name> <task-wait-time-in-seconds> <sleep-time-in-seconds>";
    }
}
