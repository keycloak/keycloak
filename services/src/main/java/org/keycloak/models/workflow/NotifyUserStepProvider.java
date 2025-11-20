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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_AFTER;

public class NotifyUserStepProvider implements WorkflowStepProvider {

    private static final String ACCOUNT_DISABLE_NOTIFICATION_SUBJECT = "accountDisableNotificationSubject";
    private static final String ACCOUNT_DELETE_NOTIFICATION_SUBJECT = "accountDeleteNotificationSubject";
    private static final String ACCOUNT_DISABLE_NOTIFICATION_BODY = "accountDisableNotificationBody";
    private static final String ACCOUNT_DELETE_NOTIFICATION_BODY = "accountDeleteNotificationBody";

    private final KeycloakSession session;
    private final ComponentModel stepModel;
    private final Logger log = Logger.getLogger(NotifyUserStepProvider.class);

    public NotifyUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class).setRealm(realm);

        String subjectKey = getSubjectKey();
        String bodyTemplate = getBodyTemplate();
        Map<String, Object> bodyAttributes = getBodyAttributes();
        UserModel user = session.users().getUserById(realm, context.getResourceId());

        if (user != null && user.getEmail() != null) {
            try {
                emailProvider.setUser(user).send(subjectKey, bodyTemplate, bodyAttributes);
                log.debugv("Notification email sent to user {0} ({1})", user.getUsername(), user.getEmail());
            } catch (EmailException e) {
                log.errorv(e, "Failed to send notification email to user {0} ({1})", user.getUsername(), user.getEmail());
            }
        } else if (user != null && user.getEmail() == null) {
            log.warnv("User {0} has no email address, skipping notification", user.getUsername());
        }
    }

    private String getSubjectKey() {
        String nextStepType = getNextStepType();
        String customSubjectKey = stepModel.getConfig().getFirst("custom_subject_key");
        
        if (customSubjectKey != null && !customSubjectKey.trim().isEmpty()) {
            return customSubjectKey;
        }
        
        // Return default subject key based on next step type
        return getDefaultSubjectKey(nextStepType);
    }

    private String getBodyTemplate() {
        return "workflow-notification.ftl";
    }

    private Map<String, Object> getBodyAttributes() {
        RealmModel realm = session.getContext().getRealm();
        Map<String, Object> attributes = new HashMap<>();
        
        String nextStepType = getNextStepType();
        
        // Custom message override or default based on step type
        String customMessage = stepModel.getConfig().getFirst("custom_message");
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            attributes.put("messageKey", "customMessage");
            attributes.put("customMessage", customMessage);
        } else {
            attributes.put("messageKey", getDefaultMessageKey(nextStepType));
        }
        
        // Calculate days remaining until next step
        int daysRemaining = calculateDaysUntilNextStep();
        
        // Message parameters for internationalization
        attributes.put("daysRemaining", daysRemaining);
        attributes.put("reason", stepModel.getConfig().getFirstOrDefault("reason", "inactivity"));
        attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
        attributes.put("nextStepType", nextStepType);
        attributes.put("subjectKey", getSubjectKey());
        
        return attributes;
    }

    private String getNextStepType() {
        Map<ComponentModel, Duration> nextStepMap = getNextNonNotificationStep();
        return nextStepMap.isEmpty() ? "unknown-step" : nextStepMap.keySet().iterator().next().getProviderId();
    }

    private int calculateDaysUntilNextStep() {
        Map<ComponentModel, Duration> nextStepMap = getNextNonNotificationStep();
        if (nextStepMap.isEmpty()) {
            return 0;
        }
        Duration timeToNextStep = nextStepMap.values().iterator().next();
        return Math.toIntExact(timeToNextStep.toDays());
    }

    private Map<ComponentModel, Duration> getNextNonNotificationStep() {
        Duration timeToNextNonNotificationStep = Duration.ZERO;

        RealmModel realm = session.getContext().getRealm();
        ComponentModel workflowModel = realm.getComponent(stepModel.getParentId());
        
        List<ComponentModel> steps = realm.getComponentsStream(workflowModel.getId(), WorkflowStepProvider.class.getName())
            .sorted((a, b) -> {
                int priorityA = Integer.parseInt(a.get("priority", "0"));
                int priorityB = Integer.parseInt(b.get("priority", "0"));
                return Integer.compare(priorityA, priorityB);
            })
            .toList();
        
        // Find current step and return next non-notification step
        boolean foundCurrent = false;
        for (ComponentModel step : steps) {
            if (foundCurrent) {
                Duration duration = DurationConverter.parseDuration(step.get(CONFIG_AFTER, "0"));
                timeToNextNonNotificationStep = timeToNextNonNotificationStep.plus(duration != null ? duration : Duration.ZERO);
                if (!step.getProviderId().equals("notify-user")) {
                    // we found the next non-notification action, accumulate its time and break
                    return Map.of(step, timeToNextNonNotificationStep);
                }
            }
            if (step.getId().equals(stepModel.getId())) {
                foundCurrent = true;
            }
        }
        
        return Map.of();
    }
    
    private String getDefaultSubjectKey(String stepType) {
        return switch (stepType) {
            case DisableUserStepProviderFactory.ID -> ACCOUNT_DISABLE_NOTIFICATION_SUBJECT;
            case DeleteUserStepProviderFactory.ID -> ACCOUNT_DELETE_NOTIFICATION_SUBJECT;
            default -> "accountNotificationSubject";
        };
    }

    private String getDefaultMessageKey(String stepType) {
        return switch (stepType) {
            case DisableUserStepProviderFactory.ID -> ACCOUNT_DISABLE_NOTIFICATION_BODY;
            case DeleteUserStepProviderFactory.ID -> ACCOUNT_DELETE_NOTIFICATION_BODY;
            default -> "accountNotificationBody";
        };
    }
}
