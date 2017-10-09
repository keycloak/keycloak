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

package org.keycloak.executors;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultExecutorsProviderFactory implements ExecutorsProviderFactory {

    protected static final Logger logger = Logger.getLogger(DefaultExecutorsProviderFactory.class);

    private int DEFAULT_MIN_THREADS = 4;
    private int DEFAULT_MAX_THREADS = 16;

    private Config.Scope config;

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();


    @Override
    public ExecutorsProvider create(KeycloakSession session) {
        return new ExecutorsProvider() {

            @Override
            public ExecutorService getExecutor(String taskType) {
                return DefaultExecutorsProviderFactory.this.getExecutor(taskType, session);
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        for (ExecutorService executor : executors.values()) {
            executor.shutdown();
        }
    }

    @Override
    public String getId() {
        return "default";
    }


    // IMPL

    protected ExecutorService getExecutor(String taskType, KeycloakSession session) {
        ExecutorService existing = executors.get(taskType);

        if (existing == null) {
            synchronized (this) {
                if (!executors.containsKey(taskType)) {
                    Config.Scope currentScope = config.scope(taskType);
                    int min = DEFAULT_MIN_THREADS;
                    int max = DEFAULT_MAX_THREADS;

                    if (currentScope != null) {
                        min = currentScope.getInt("min", DEFAULT_MIN_THREADS);
                        max = currentScope.getInt("max", DEFAULT_MAX_THREADS);
                    }

                    logger.debugf("Creating pool for task '%s': min=%d, max=%d", taskType, min, max);
                    ExecutorService executor = createPool(taskType, session, min, max);
                    executors.put(taskType, executor);
                }

                existing = executors.get(taskType);
            }
        }

        return existing;
    }


    protected ExecutorService createPool(String taskType, KeycloakSession session, int min, int max) {
        ThreadFactory threadFactory = new ThreadFactory() {

            private AtomicInteger i = new AtomicInteger(0);
            private int group = new Random().nextInt(2048);

            @Override
            public Thread newThread(Runnable r) {
                int threadNumber = i.getAndIncrement();
                String threadName = "kc-" + taskType + "-" + group + "-" + threadNumber;

                if (logger.isTraceEnabled()) {
                    logger.tracef("Creating thread: %s", threadName);
                }

                return new Thread(r, threadName);
            }

        };

        if (min == max) {
            return Executors.newFixedThreadPool(min, threadFactory);
        } else {
            // Same like Executors.newCachedThreadPool. Besides that "min" and "max" are configurable
            return new ThreadPoolExecutor(min, max,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    threadFactory);
        }
    }

}
