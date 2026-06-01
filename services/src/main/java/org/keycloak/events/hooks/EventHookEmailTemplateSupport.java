/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;

final class EventHookEmailTemplateSupport {

    static final String ID = "email";
    static final String RECIPIENT_TEMPLATE = "recipientTemplate";
    static final String SUBJECT_TEMPLATE = "subjectTemplate";
    static final String TEXT_BODY_TEMPLATE = "textBodyTemplate";
    static final String HTML_BODY_TEMPLATE = "htmlBodyTemplate";
    static final String LOCALE_TEMPLATE = "localeTemplate";
    static final String SENT_STATUS_CODE = "EMAIL_SENT";
    static final String SEND_FAILED_STATUS_CODE = "EMAIL_SEND_FAILED";
    static final String SUBJECT_MESSAGE_KEY = "eventHookEmailSubject";
    static final String BODY_TEMPLATE_NAME = "event-hook-notification.ftl";

    private EventHookEmailTemplateSupport() {
    }

    static void validateConfig(Map<String, Object> settings) {
        requireSetting(settings, RECIPIENT_TEMPLATE);
        requireSetting(settings, SUBJECT_TEMPLATE);

        String textBodyTemplate = optionalSetting(settings, TEXT_BODY_TEMPLATE);
        String htmlBodyTemplate = optionalSetting(settings, HTML_BODY_TEMPLATE);
        if (textBodyTemplate == null && htmlBodyTemplate == null) {
            throw new IllegalArgumentException("At least one email body template must be configured");
        }

        EventHookBodyMappingSupport.validateTemplate(requireSetting(settings, RECIPIENT_TEMPLATE), "email recipient template");
        EventHookBodyMappingSupport.validateTemplate(requireSetting(settings, SUBJECT_TEMPLATE), "email subject template");

        if (textBodyTemplate != null) {
            EventHookBodyMappingSupport.validateTemplate(textBodyTemplate, "email text body template");
        }
        if (htmlBodyTemplate != null) {
            EventHookBodyMappingSupport.validateTemplate(htmlBodyTemplate, "email html body template");
        }

        String localeTemplate = optionalSetting(settings, LOCALE_TEMPLATE);
        if (localeTemplate != null) {
            EventHookBodyMappingSupport.validateTemplate(localeTemplate, "email locale template");
        }
    }

    static RenderedEmail render(KeycloakSession session, RealmModel realm, EventHookTargetModel target,
            List<EventHookMessageModel> messages) throws EventHookBodyMappingSupport.EventHookBodyMappingException, IOException {
        List<Object> payloads = messages.stream().map(message -> {
            try {
                return EventHookBodyMappingSupport.readPayload(message.getPayload());
            } catch (EventHookBodyMappingSupport.EventHookBodyMappingException exception) {
                throw new IllegalStateException(exception);
            }
        }).toList();

        try {
            return renderPayloads(session, realm, target, messages, payloads);
        } catch (IllegalStateException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof EventHookBodyMappingSupport.EventHookBodyMappingException mappingException) {
                throw mappingException;
            }
            throw exception;
        }
    }

    private static RenderedEmail renderPayloads(KeycloakSession session, RealmModel realm, EventHookTargetModel target,
            List<EventHookMessageModel> messages, List<Object> payloads) throws EventHookBodyMappingSupport.EventHookBodyMappingException, IOException {
        Map<String, Object> model = payloads.size() == 1
                ? EventHookBodyMappingSupport.singleEventModel(payloads.get(0))
                : EventHookBodyMappingSupport.batchEventModel(payloads);

        EventHookMessageModel representativeMessage = messages.get(0);
        UserModel recipientUser = resolveRecipientUser(session, realm, messages);
        model.put("message", toMessageModel(representativeMessage));
        model.put("messages", messages.stream().map(EventHookEmailTemplateSupport::toMessageModel).toList());
        model.put("target", toTargetModel(target));
        model.put("realm", toRealmModel(realm, target));
        model.put("user", toUserModel(recipientUser));
        model.put("recipientUser", toUserModel(recipientUser));

        Locale locale = resolveLocale(session, realm, target, recipientUser, model);
        Theme theme = session.theme().getTheme(Theme.Type.EMAIL);
        Properties messagesBundle = theme.getEnhancedMessages(realm, locale);

        model.put("locale", Map.of("language", locale.getLanguage(), "tag", locale.toLanguageTag()));
        model.put("realmName", realmDisplayName(realm, target));
        model.put("msg", new MessageFormatterMethod(locale, messagesBundle));
        model.put("properties", theme.getProperties());

        Map<String, Object> settings = target.getSettings();
        String recipient = renderRequiredTemplate(settings, RECIPIENT_TEMPLATE, model, "email recipient template");
        String subject = renderRequiredTemplate(settings, SUBJECT_TEMPLATE, model, "email subject template");
        String textBody = renderOptionalTemplate(settings, TEXT_BODY_TEMPLATE, model);
        String htmlBody = renderOptionalTemplate(settings, HTML_BODY_TEMPLATE, model);

        return new RenderedEmail(recipient, subject, textBody == null ? "" : textBody.trim(),
                htmlBody == null ? "" : htmlBody.trim(), locale, recipientUser);
    }

    private static Locale resolveLocale(KeycloakSession session, RealmModel realm, EventHookTargetModel target, UserModel recipientUser,
            Map<String, Object> model) throws EventHookBodyMappingSupport.EventHookBodyMappingException {
        String configuredLocaleTemplate = optionalSetting(target.getSettings(), LOCALE_TEMPLATE);
        if (configuredLocaleTemplate != null) {
            String renderedLocale = EventHookBodyMappingSupport.renderTemplate(configuredLocaleTemplate, model).trim();
            if (renderedLocale.isEmpty()) {
                throw new EventHookBodyMappingSupport.EventHookBodyMappingException("Email locale template rendered an empty locale");
            }

            Locale locale = Locale.forLanguageTag(renderedLocale);
            if (locale.getLanguage() == null || locale.getLanguage().isBlank()) {
                throw new EventHookBodyMappingSupport.EventHookBodyMappingException("Email locale template rendered an invalid locale: " + renderedLocale);
            }
            return locale;
        }

        if (session != null && session.getContext() != null) {
            Locale locale = session.getContext().resolveLocale(recipientUser);
            if (locale != null) {
                return locale;
            }
        }

        return Locale.ENGLISH;
    }

    private static UserModel resolveRecipientUser(KeycloakSession session, RealmModel realm, List<EventHookMessageModel> messages) {
        if (session == null || realm == null || messages.isEmpty()) {
            return null;
        }

        String userId = messages.get(0).getUserId();
        if (userId == null || userId.isBlank()) {
            return null;
        }

        boolean sameUser = messages.stream().map(EventHookMessageModel::getUserId).allMatch(userId::equals);
        if (!sameUser) {
            return null;
        }

        return session.users().getUserById(realm, userId);
    }

    private static String renderRequiredTemplate(Map<String, Object> settings, String key, Map<String, Object> model,
            String description) throws EventHookBodyMappingSupport.EventHookBodyMappingException {
        String template = requireSetting(settings, key);
        String rendered = EventHookBodyMappingSupport.renderTemplate(template, model).trim();
        if (rendered.isEmpty()) {
            throw new EventHookBodyMappingSupport.EventHookBodyMappingException(description + " rendered an empty value");
        }
        return rendered;
    }

    private static String renderOptionalTemplate(Map<String, Object> settings, String key, Map<String, Object> model)
            throws EventHookBodyMappingSupport.EventHookBodyMappingException {
        String template = optionalSetting(settings, key);
        if (template == null) {
            return null;
        }
        return EventHookBodyMappingSupport.renderTemplate(template, model);
    }

    private static String requireSetting(Map<String, Object> settings, String key) {
        Object configured = settings == null ? null : settings.get(key);
        if (configured == null) {
            throw new IllegalArgumentException("Missing required setting: " + key);
        }

        String value = configured.toString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing required setting: " + key);
        }
        return value;
    }

    private static String optionalSetting(Map<String, Object> settings, String key) {
        Object configured = settings == null ? null : settings.get(key);
        if (configured == null) {
            return null;
        }

        String value = configured.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private static Map<String, Object> toMessageModel(EventHookMessageModel message) {
        if (message == null) {
            return Map.of();
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", message.getId());
        model.put("realmId", message.getRealmId());
        model.put("targetId", message.getTargetId());
        model.put("executionId", message.getExecutionId());
        model.put("sourceType", message.getSourceType() == null ? null : message.getSourceType().name());
        model.put("sourceEventId", message.getSourceEventId());
        model.put("sourceEventName", message.getSourceEventName());
        model.put("userId", message.getUserId());
        model.put("resourcePath", message.getResourcePath());
        model.put("status", message.getStatus() == null ? null : message.getStatus().name());
        model.put("attemptCount", message.getAttemptCount());
        model.put("nextAttemptAt", message.getNextAttemptAt());
        model.put("executionStartedAt", message.getExecutionStartedAt());
        model.put("createdAt", message.getCreatedAt());
        model.put("updatedAt", message.getUpdatedAt());
        model.put("lastError", message.getLastError());
        model.put("executionBatch", message.isExecutionBatch());
        model.put("test", message.isTest());
        return model;
    }

    private static Map<String, Object> toTargetModel(EventHookTargetModel target) {
        if (target == null) {
            return Map.of();
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", target.getId());
        model.put("realmId", target.getRealmId());
        model.put("realmName", target.getRealmName());
        model.put("name", target.getName());
        model.put("type", target.getType());
        model.put("enabled", target.isEnabled());
        model.put("settings", target.getSettings() == null ? Map.of() : target.getSettings());
        return model;
    }

    private static Map<String, Object> toRealmModel(RealmModel realm, EventHookTargetModel target) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", realm != null ? realm.getId() : target == null ? null : target.getRealmId());
        model.put("name", realm != null ? realm.getName() : target == null ? null : target.getRealmName());
        model.put("displayName", realm != null ? realm.getDisplayName() : null);
        return model;
    }

    private static Map<String, Object> toUserModel(UserModel user) {
        if (user == null) {
            return Map.of();
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", user.getId());
        model.put("username", user.getUsername());
        model.put("firstName", user.getFirstName());
        model.put("lastName", user.getLastName());
        model.put("email", user.getEmail());
        model.put("emailVerified", user.isEmailVerified());
        model.put("enabled", user.isEnabled());
        model.put("attributes", user.getAttributes() == null ? Map.of() : user.getAttributes());
        return model;
    }

    private static String realmDisplayName(RealmModel realm, EventHookTargetModel target) {
        if (realm != null && realm.getDisplayName() != null && !realm.getDisplayName().isBlank()) {
            return realm.getDisplayName();
        }
        if (realm != null && realm.getName() != null && !realm.getName().isBlank()) {
            return realm.getName();
        }
        return target == null ? "Keycloak" : Objects.toString(target.getRealmName(), "Keycloak");
    }

    static final class RenderedEmail {

        private final String recipient;
        private final String subject;
        private final String textBody;
        private final String htmlBody;
        private final Locale locale;
        private final UserModel user;

        RenderedEmail(String recipient, String subject, String textBody, String htmlBody, Locale locale, UserModel user) {
            this.recipient = recipient;
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
            this.locale = locale;
            this.user = user;
        }

        String recipient() {
            return recipient;
        }

        String subject() {
            return subject;
        }

        String textBody() {
            return textBody;
        }

        String htmlBody() {
            return htmlBody;
        }

        Locale locale() {
            return locale;
        }

        UserModel user() {
            return user;
        }
    }
}
