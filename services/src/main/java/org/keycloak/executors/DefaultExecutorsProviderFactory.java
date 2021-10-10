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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultExecutorsProviderFactory implements ExecutorsProviderFactory {

    protected static final Logger logger = Logger.getLogger(DefaultExecutorsProviderFactory.class);

    private static final int DEFAULT_MIN_THREADS = 4;
    private static final int DEFAULT_MAX_THREADS = 64;

    private static final String MANAGED_EXECUTORS_SERVICE_JNDI_PREFIX = "java:jboss/ee/concurrency/executor/";

    // Default executor is bound on Wildfly under this name
    private static final String DEFAULT_MANAGED_EXECUTORS_SERVICE_JNDI = MANAGED_EXECUTORS_SERVICE_JNDI_PREFIX + "default";

    private Config.Scope config;

    private Boolean managed = null;

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
        if (managed != null && !managed) {
            for (Map.Entry<String, ExecutorService> executor : executors.entrySet()) {
                logger.debugf("Shutting down executor for task '%s'", executor.getKey());
                executor.getValue().shutdown();
            }
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
                    ExecutorService executor = retrievePool(taskType, session);
                    executors.put(taskType, executor);
                }

                existing = executors.get(taskType);
            }
        }

        return existing;
    }


    protected ExecutorService retrievePool(String taskType, KeycloakSession session) {
        if (managed == null) {
            detectManaged();
        }

        if (managed) {
            return getPoolManaged(taskType, session);
        } else {
            return createPoolEmbedded(taskType, session);
        }
    }

    protected void detectManaged() {
        String jndiName = MANAGED_EXECUTORS_SERVICE_JNDI_PREFIX + "default";
        try {
            new InitialContext().lookup(jndiName);
            logger.debugf("We are in managed environment. Executor '%s' was available.", jndiName);
            managed = true;
        } catch (NamingException nnfe) {
            logger.debugf("We are not in managed environment. Executor '%s' was not available.", jndiName);
            managed = false;
        }
    }


    protected ExecutorService getPoolManaged(String taskType, KeycloakSession session) {
        try {
            InitialContext ctx = new InitialContext();

            // First check if specific pool for the task
            String jndiName = MANAGED_EXECUTORS_SERVICE_JNDI_PREFIX + taskType;
            try {
                ExecutorService executor = (ExecutorService) ctx.lookup(jndiName);
                logger.debugf("Found executor for '%s' under JNDI name '%s'", taskType, jndiName);
                return executor;
            } catch (NameNotFoundException nnfe) {
                logger.debugf("Not found executor for '%s' under specific JNDI name '%s'. Fallback to the default pool", taskType, jndiName);

                ExecutorService executor = (ExecutorService) ctx.lookup(DEFAULT_MANAGED_EXECUTORS_SERVICE_JNDI);
                logger.debugf("Found default executor for '%s' of JNDI name '%s'", taskType, DEFAULT_MANAGED_EXECUTORS_SERVICE_JNDI);
                return executor;
            }
        } catch (NamingException ne) {
            throw new IllegalStateException(ne);
        }
    }


    protected ExecutorService createPoolEmbedded(String taskType, KeycloakSession session) {
        Config.Scope currentScope = config.scope(taskType);
        int min = DEFAULT_MIN_THREADS;
        int max = DEFAULT_MAX_THREADS;

        if (currentScope != null) {
            min = currentScope.getInt("min", DEFAULT_MIN_THREADS);
            max = currentScope.getInt("max", DEFAULT_MAX_THREADS);
        }

        logger.debugf("Creating pool for task '%s': min=%d, max=%d", taskType, min, max);

        ThreadFactory threadFactory = createThreadFactory(taskType, session);

        if (min == max) {
            return Executors.newFixedThreadPool(min, threadFactory);
        } else {
            // Same like Executors.newCachedThreadPool. Besides that "min" and "max" are configurable
            return new ThreadPoolExecutor(min, max,
                    60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(1024),
                    threadFactory);
        }
    }


    protected ThreadFactory createThreadFactory(String taskType, KeycloakSession session) {
        return new ThreadFactory() {

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
    }

}
