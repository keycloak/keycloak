/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.workflow;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.util.DurationConverter;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class WorkflowsEventListenerFactory implements EventListenerProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(WorkflowsEventListenerFactory.class);

    public static final String ID = "workflow-event-listener";
    private static final long DEFAULT_STEP_RUNNER_TASK_INTERVAL = Duration.ofHours(12).toMillis();
    private long stepRunnerTaskInterval;
    private LocalTime stepRunnerTaskStartTime;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new WorkflowEventListener(session);
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void init(Scope config) {
        String taskIntervalStr = config.get("stepRunnerTaskInterval");
        this.stepRunnerTaskInterval = taskIntervalStr == null ? DEFAULT_STEP_RUNNER_TASK_INTERVAL : DurationConverter.parseDuration(taskIntervalStr).toMillis();

        String startTimeStr = config.get("stepRunnerTaskStartTime");
        if (startTimeStr != null) {
            try {
                this.stepRunnerTaskStartTime = LocalTime.parse(startTimeStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid stepRunnerTaskStartTime value '" + startTimeStr
                        + "'. Expected format: HH:mm (e.g., 02:00, 14:30)", e);
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            KeycloakSession session = event.getKeycloakSession();

            if (session != null) {
                onEvent(event, session);
            }
        });
        scheduleStepRunnerTask(factory);
    }

    private void onEvent(ProviderEvent event, KeycloakSession session) {
        WorkflowEventListener provider = (WorkflowEventListener) session.getProvider(EventListenerProvider.class, getId());
        provider.onEvent(event);
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.WORKFLOWS);
    }

    private void scheduleStepRunnerTask(KeycloakSessionFactory factory) {
        long initialDelay = computeInitialDelay();

        try (KeycloakSession session = factory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            ClusterAwareScheduledTaskRunner runner = new ClusterAwareScheduledTaskRunner(factory,
                    new WorkflowRunnerScheduledTask(factory), stepRunnerTaskInterval);
            timer.schedule(runner, initialDelay, stepRunnerTaskInterval);
        }

        ZonedDateTime nextExecution = ZonedDateTime.now().plus(Duration.ofMillis(initialDelay));
        logger.infof("Workflow runner task scheduled: next execution at %s, then every %s",
                nextExecution.toLocalTime().withNano(0),
                Duration.ofMillis(stepRunnerTaskInterval));
    }

    /**
     * Computes the initial delay before the first task execution.
     * <p>
     * If a start time is configured, it is used as an anchor to align executions to a predictable
     * schedule. For example, with a start time of 18:00 and an interval of 2 hours, the execution
     * grid is 00:00, 02:00, 04:00, ..., 16:00, 18:00, 20:00, 22:00. The initial delay is calculated
     * so that the first execution occurs at the next grid point after the current time.
     * <p>
     * If no start time is configured, the initial delay equals the interval (current default behavior).
     */
    long computeInitialDelay() {
        if (stepRunnerTaskStartTime == null) {
            return stepRunnerTaskInterval;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime anchor = now.toLocalDate().atTime(stepRunnerTaskStartTime).atZone(now.getZone());
        long millisPastLastGridPoint = Math.floorMod(Duration.between(anchor, now).toMillis(), stepRunnerTaskInterval);
        return stepRunnerTaskInterval - millisPastLastGridPoint;
    }
}
