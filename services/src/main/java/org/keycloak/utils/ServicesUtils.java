/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import org.jboss.logging.Logger;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 * Utility class for general helper methods used across the keycloak-services.
 * @deprecated - DELETE once only used from within legacy datastore module
 */
public class ServicesUtils {

    private static final Logger logger = Logger.getLogger(ServicesUtils.class);

    public static <T, R> Function<? super T,? extends Stream<? extends R>> timeBound(KeycloakSession session,
                                                                                     long timeout,
                                                                                     Function<T, ? extends Stream<R>> func) {
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("storage-provider-threads");
        return p -> {
            // We are running another thread here, which serves as a time checking thread. When timeout is hit, the time
            // checking thread will send interrupted flag to main thread, which can cause interruption of func execution.
            // To support interruption func implementation should react to interrupt flag.
            // If func doesn't check the interrupted flag, the execution won't be interrupted and can take more time
            // than the threshold given by timeout variable
            Future<?> timeCheckingThread = executor.submit(timeWarningRunnable(timeout, Thread.currentThread()));
            try {
                // We cannot run func in different than main thread, because main thread have, for example, EntityManager
                // transaction context. If we run any operation on EntityManager in a different thread, it will fail
                // with a transaction doesn't exist error
                return func.apply(p);
            } finally {
                timeCheckingThread.cancel(true);

                if (Thread.interrupted()) {
                    logger.warnf("Execution with object [%s] exceeded specified time limit %d. %s", p, timeout, getShortStackTrace());
                }
            }
        };
    }

    public static <T, R> Function<? super T, R> timeBoundOne(KeycloakSession session,
                                                                                     long timeout,
                                                                                     Function<T, R> func) {
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("storage-provider-threads");
        return p -> {
            // We are running another thread here, which serves as a time checking thread. When timeout is hit, the time
            // checking thread will send interrupted flag to main thread, which can cause interruption of func execution.
            // To support interruption func implementation should react to interrupt flag.
            // If func doesn't check the interrupted flag, the execution won't be interrupted and can take more time
            // than the threshold given by timeout variable
            Future<?> warningThreadFuture = executor.submit(timeWarningRunnable(timeout, Thread.currentThread()));
            try {
                // We cannot run func in different than main thread, because main thread have, for example, EntityManager
                // transaction context. If we run any operation on EntityManager in a different thread, it will fail
                // with a transaction doesn't exist error
                return func.apply(p);
            } finally {
                warningThreadFuture.cancel(true);

                if (Thread.interrupted()) {
                    logger.warnf("Execution with object [%s] exceeded specified time limit %d. %s", p, timeout, getShortStackTrace());
                }
            }
        };
    }

    public static <T> Consumer<? super T> consumeWithTimeBound(KeycloakSession session,
                                                             long timeout,
                                                             Consumer<T> func) {
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("storage-provider-threads");
        return p -> {
            // We are running another thread here, which serves as a time checking thread. When timeout is hit, the time
            // checking thread will send interrupted flag to main thread, which can cause interruption of func execution.
            // To support interruption func implementation should react to interrupt flag.
            // If func doesn't check the interrupted flag, the execution won't be interrupted and can take more time
            // than the threshold given by timeout variable
            Future<?> warningThreadFuture = executor.submit(timeWarningRunnable(timeout, Thread.currentThread()));
            try {
                // We cannot run func in different than main thread, because main thread have, for example, EntityManager
                // transaction context. If we run any operation on EntityManager in a different thread, it will fail
                // with a transaction doesn't exist error
                func.accept(p);
            } finally {
                warningThreadFuture.cancel(true);

                if (Thread.interrupted()) {
                    logger.warnf("Execution with object [%s] exceeded specified time limit %d. %s", p, timeout, getShortStackTrace());
                }
            }
        };
    }

    private static Runnable timeWarningRunnable(long timeout, Thread mainThread) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException exception) {
                    return; // Do not interrupt if warning thread was interrupted (== main thread finished execution in time)
                }

                mainThread.interrupt();
            }
        };
    }
}
