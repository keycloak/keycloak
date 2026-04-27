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

package org.keycloak.models.sessions.infinispan.util;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jboss.logging.Logger;

/**
 * Not thread-safe. Assumes tasks are added from single thread.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FuturesHelper {

    private static final Logger log = Logger.getLogger(FuturesHelper.class);

    private final Queue<Future> futures = new LinkedList<>();


    public void addTask(Future future) {
        this.futures.add(future);
    }


    public void waitForAllToFinish() {
        for (Future future : futures) {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException ee) {
                log.error("Exception when waiting for future", ee); // TODO Possibly some good mechanism to avoid swamp log with many same exceptions?
            }
        }
    }


    public int size() {
        return futures.size();
    }

}
