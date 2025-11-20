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

package org.keycloak.timer;

import java.util.Map;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface TimerProvider extends Provider {

    public void schedule(Runnable runnable, long intervalMillis, String taskName);

    default void schedule(TaskRunner runner, long intervalMillis) {
        schedule(runner, intervalMillis, runner.getTaskName());
    }

    public void scheduleTask(ScheduledTask scheduledTask, long intervalMillis, String taskName);

    public default void scheduleTask(ScheduledTask scheduledTask, long intervalMillis) {
        scheduleTask(scheduledTask, intervalMillis, scheduledTask.getTaskName());
    }

    /**
     * Cancel task and return the details about it, so it can be eventually restored later
     *
     * @param taskName
     * @return existing task or null if task under this name doesn't exist
     */
    public TimerTaskContext cancelTask(String taskName);

    public Map<String, TimerTaskContext> getTasks();

    interface TimerTaskContext {

        Runnable getRunnable();

        long getStartTimeMillis();

        long getIntervalMillis();
    }

}
