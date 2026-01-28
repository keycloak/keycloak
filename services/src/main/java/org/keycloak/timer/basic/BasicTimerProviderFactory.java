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

package org.keycloak.timer.basic;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BasicTimerProviderFactory implements TimerProviderFactory {

    private Timer timer;

    private int transactionTimeout;

    public static final String TRANSACTION_TIMEOUT = "transactionTimeout";

    private ConcurrentMap<String, TimerTaskContextImpl> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public TimerProvider create(KeycloakSession session) {
        return new BasicTimerProvider(session, timer, transactionTimeout, this);
    }

    @Override
    public void init(Config.Scope config) {
        transactionTimeout = config.getInt(TRANSACTION_TIMEOUT, 0);
        timer = new Timer();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        timer.cancel();
        timer = null;
    }

    @Override
    public String getId() {
        return "basic";
    }

    protected TimerTaskContextImpl putTask(String taskName, TimerTaskContextImpl task) {
        return scheduledTasks.put(taskName, task);
    }

    protected TimerTaskContextImpl removeTask(String taskName) {
        return scheduledTasks.remove(taskName);
    }

    protected Map<String, TimerTaskContextImpl> getTasks(){
        return scheduledTasks;
    }
}
