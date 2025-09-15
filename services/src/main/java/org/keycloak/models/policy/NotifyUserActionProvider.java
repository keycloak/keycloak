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

package org.keycloak.models.policy;

import static org.keycloak.models.policy.ResourceAction.AFTER_KEY;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class NotifyUserActionProvider implements ResourceActionProvider {

    private static final String ACCOUNT_DISABLE_NOTIFICATION_SUBJECT = "accountDisableNotificationSubject";
    private static final String ACCOUNT_DELETE_NOTIFICATION_SUBJECT = "accountDeleteNotificationSubject";
    private static final String ACCOUNT_DISABLE_NOTIFICATION_BODY = "accountDisableNotificationBody";
    private static final String ACCOUNT_DELETE_NOTIFICATION_BODY = "accountDeleteNotificationBody";

    private final KeycloakSession session;
    private final ComponentModel actionModel;
    private final Logger log = Logger.getLogger(NotifyUserActionProvider.class);

    public NotifyUserActionProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.actionModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(List<String> userIds) {
        RealmModel realm = session.getContext().getRealm();
        EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class).setRealm(realm);

        String subjectKey = getSubjectKey();
        String bodyTemplate = getBodyTemplate();
        Map<String, Object> bodyAttributes = getBodyAttributes();

        for (String id : userIds) {
            UserModel user = session.users().getUserById(realm, id);

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
    }

    private String getSubjectKey() {
        String nextActionType = getNextActionType();
        String customSubjectKey = actionModel.getConfig().getFirst("custom_subject_key");
        
        if (customSubjectKey != null && !customSubjectKey.trim().isEmpty()) {
            return customSubjectKey;
        }
        
        // Return default subject key based on next action type
        return getDefaultSubjectKey(nextActionType);
    }

    private String getBodyTemplate() {
        return "resource-policy-notification.ftl";
    }

    private Map<String, Object> getBodyAttributes() {
        RealmModel realm = session.getContext().getRealm();
        Map<String, Object> attributes = new HashMap<>();
        
        String nextActionType = getNextActionType();
        
        // Custom message override or default based on action type
        String customMessage = actionModel.getConfig().getFirst("custom_message");
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            attributes.put("messageKey", "customMessage");
            attributes.put("customMessage", customMessage);
        } else {
            attributes.put("messageKey", getDefaultMessageKey(nextActionType));
        }
        
        // Calculate days remaining until next action
        int daysRemaining = calculateDaysUntilNextAction();
        
        // Message parameters for internationalization
        attributes.put("daysRemaining", daysRemaining);
        attributes.put("reason", actionModel.getConfig().getFirstOrDefault("reason", "inactivity"));
        attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
        attributes.put("nextActionType", nextActionType);
        attributes.put("subjectKey", getSubjectKey());
        
        return attributes;
    }

    private String getNextActionType() {
        Map<ComponentModel, Long> nextActionMap = getNextNonNotificationAction();
        return nextActionMap.isEmpty() ? "unknown-action" : nextActionMap.keySet().iterator().next().getProviderId();
    }

    private int calculateDaysUntilNextAction() {
        Map<ComponentModel, Long> nextActionMap = getNextNonNotificationAction();
        if (nextActionMap.isEmpty()) {
            return 0;
        }
        Long timeToNextAction = nextActionMap.values().iterator().next();
        return Math.toIntExact(Duration.ofMillis(timeToNextAction).toDays());
    }

    private Map<ComponentModel, Long> getNextNonNotificationAction() {
        long timeToNextNonNotificationAction = 0L;

        RealmModel realm = session.getContext().getRealm();
        ComponentModel policyModel = realm.getComponent(actionModel.getParentId());
        
        List<ComponentModel> actions = realm.getComponentsStream(policyModel.getId(), ResourceActionProvider.class.getName())
            .sorted((a, b) -> {
                int priorityA = Integer.parseInt(a.get("priority", "0"));
                int priorityB = Integer.parseInt(b.get("priority", "0"));
                return Integer.compare(priorityA, priorityB);
            })
            .toList();
        
        // Find current action and return next non-notification action
        boolean foundCurrent = false;
        for (ComponentModel action : actions) {
            if (foundCurrent) {
                timeToNextNonNotificationAction += action.get(AFTER_KEY, 0L);
                if (!action.getProviderId().equals("notify-user-action-provider")) {
                    // we found the next non-notification action, accumulate its time and break
                    return Map.of(action, timeToNextNonNotificationAction);
                }
            }
            if (action.getId().equals(actionModel.getId())) {
                foundCurrent = true;
            }
        }
        
        return Map.of();
    }
    
    private String getDefaultSubjectKey(String actionType) {
        return switch (actionType) {
            case "disable-user-action-provider" -> ACCOUNT_DISABLE_NOTIFICATION_SUBJECT;
            case "delete-user-action-provider" -> ACCOUNT_DELETE_NOTIFICATION_SUBJECT;
            default -> "accountNotificationSubject";
        };
    }

    private String getDefaultMessageKey(String actionType) {
        return switch (actionType) {
            case "disable-user-action-provider" -> ACCOUNT_DISABLE_NOTIFICATION_BODY;
            case "delete-user-action-provider" -> ACCOUNT_DELETE_NOTIFICATION_BODY;
            default -> "accountNotificationBody";
        };
    }

    @Override
    public boolean isRunnable() {
        return actionModel.get(AFTER_KEY) != null;
    }
}
