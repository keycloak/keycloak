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
import java.text.Bidi;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.context.ContextNotActiveException;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailEventHookTargetProvider implements EventHookTargetProvider {

    private static final Logger LOG = LoggerFactory.getLogger(EmailEventHookTargetProvider.class);

    private final KeycloakSession session;
    private final EmailDispatcher emailDispatcher;

    public EmailEventHookTargetProvider(KeycloakSession session) {
        this(session, new RealmEmailDispatcher(session));
    }

    EmailEventHookTargetProvider(KeycloakSession session, EmailDispatcher emailDispatcher) {
        this.session = session;
        this.emailDispatcher = emailDispatcher;
    }

    @Override
    public EventHookDeliveryResult deliver(EventHookTargetModel target, EventHookMessageModel message) throws IOException {
        return deliverMessages(target, List.of(message));
    }

    @Override
    public EventHookDeliveryResult deliverBatch(EventHookTargetModel target, List<EventHookMessageModel> messages) throws IOException {
        return deliverMessages(target, messages);
    }

    private EventHookDeliveryResult deliverMessages(EventHookTargetModel target, List<EventHookMessageModel> messages) throws IOException {
        long started = System.currentTimeMillis();
        try {
            RealmModel realm = resolveRealm(target);
            if (!isSmtpConfigured(realm)) {
                String details = "Realm SMTP is not configured for the email event hook target";
                LOG.error("Failed to deliver email event hook for realm '{}' target '{}' ({} message(s)): {}",
                        realmName(realm, target),
                        target == null ? null : target.getId(),
                        messages == null ? 0 : messages.size(),
                        details);
                return sendFailedResult(started, details, false);
            }

            EventHookEmailTemplateSupport.RenderedEmail email = EventHookEmailTemplateSupport.render(session, realm, target, messages);
            emailDispatcher.send(realm, email);

            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setSuccess(true);
            result.setRetryable(false);
            result.setStatusCode(EventHookEmailTemplateSupport.SENT_STATUS_CODE);
            result.setDetails(truncate("Email sent to " + email.recipient(), 1024));
            result.setDurationMillis(Math.max(0, System.currentTimeMillis() - started));
            return result;
        } catch (EventHookBodyMappingSupport.EventHookBodyMappingException exception) {
            return parseFailedResult(started, exception);
        } catch (EmailException exception) {
            return sendFailedResult(started, exception.getMessage(), true);
        }
    }

    private RealmModel resolveRealm(EventHookTargetModel target) {
        if (session == null || target == null || target.getRealmId() == null || target.getRealmId().isBlank()) {
            return session == null || session.getContext() == null ? null : session.getContext().getRealm();
        }
        return session.realms().getRealm(target.getRealmId());
    }

    private EventHookDeliveryResult parseFailedResult(long started, EventHookBodyMappingSupport.EventHookBodyMappingException exception) {
        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(false);
        result.setStatusCode(EventHookBodyMappingSupport.PARSE_FAILED_STATUS_CODE);
        result.setDetails(truncate(exception.getMessage(), 1024));
        result.setDurationMillis(Math.max(0, System.currentTimeMillis() - started));
        return result;
    }

    private EventHookDeliveryResult sendFailedResult(long started, String details, boolean retryable) {
        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(retryable);
        result.setStatusCode(EventHookEmailTemplateSupport.SEND_FAILED_STATUS_CODE);
        result.setDetails(truncate(details, 1024));
        result.setDurationMillis(Math.max(0, System.currentTimeMillis() - started));
        return result;
    }

    private boolean isSmtpConfigured(RealmModel realm) {
        if (realm == null || realm.getSmtpConfig() == null || realm.getSmtpConfig().isEmpty()) {
            return false;
        }

        return hasText(realm.getSmtpConfig().get("host")) && hasText(realm.getSmtpConfig().get("from"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String realmName(RealmModel realm, EventHookTargetModel target) {
        if (realm != null && realm.getName() != null && !realm.getName().isBlank()) {
            return realm.getName();
        }
        return target == null ? null : target.getRealmId();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Override
    public void close() {
    }

    interface EmailDispatcher {
        void send(RealmModel realm, EventHookEmailTemplateSupport.RenderedEmail email) throws EmailException;
    }

    static final class RealmEmailDispatcher implements EmailDispatcher {

        private final KeycloakSession session;

        RealmEmailDispatcher(KeycloakSession session) {
            this.session = session;
        }

        @Override
        public void send(RealmModel realm, EventHookEmailTemplateSupport.RenderedEmail email) throws EmailException {
            InlineEmailTemplateProvider provider = new InlineEmailTemplateProvider(session)
                    .setRealm(realm)
                    .setUser(email.user());

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("renderedTextBody", email.textBody());
            attributes.put("renderedHtmlBody", email.htmlBody());
            provider.send(EventHookEmailTemplateSupport.SUBJECT_MESSAGE_KEY,
                    List.of(email.subject()),
                    EventHookEmailTemplateSupport.BODY_TEMPLATE_NAME,
                    attributes,
                    email.recipient());
        }
    }

    static class InlineEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

        InlineEmailTemplateProvider(KeycloakSession session) {
            super(session);
        }

        @Override
        public InlineEmailTemplateProvider setRealm(RealmModel realm) {
            super.setRealm(realm);
            return this;
        }

        @Override
        public InlineEmailTemplateProvider setUser(UserModel user) {
            super.setUser(user);
            return this;
        }

        @Override
        protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template,
                Map<String, Object> attributes) throws EmailException {
            try {
                Locale locale = session.getContext().resolveLocale(user, false);
                attributes.put("locale", locale);

                Theme theme = getTheme();
                Properties messages = theme.getEnhancedMessages(realm, locale);

                String currentLanguageTag = locale.getLanguage();
                String currentLanguage = messages.getProperty("locale_" + currentLanguageTag, currentLanguageTag);
                boolean isLtr = new Bidi(currentLanguage, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isLeftToRight();
                attributes.put("ltr", isLtr);
                attributes.put("msg", new org.keycloak.theme.beans.MessageFormatterMethod(locale, messages));
                attributes.put("properties", theme.getProperties());
                attributes.put("realmName", getRealmName());
                attributes.put("user", user != null ? new ProfileBean(user, session) : Collections.emptyMap());

                try {
                    KeycloakUriInfo uriInfo = session.getContext().getUri();
                    attributes.put("url", new UrlBean(realm, theme, uriInfo.getBaseUri(), null));
                } catch (ContextNotActiveException exception) {
                    LOG.debug("No active request, can't make url attribute available to the template", exception);
                }

                String subject = new MessageFormat(messages.getProperty(subjectKey, subjectKey), locale)
                        .format(subjectAttributes.toArray());

                String textBody;
                try {
                    textBody = freeMarker.processTemplate(attributes, "text/" + template, theme);
                } catch (FreeMarkerException exception) {
                    throw new EmailException("Failed to template plain text email.", exception);
                }

                String htmlBody;
                try {
                    htmlBody = freeMarker.processTemplate(attributes, "html/" + template, theme);
                } catch (FreeMarkerException exception) {
                    throw new EmailException("Failed to template html email.", exception);
                }

                return new EmailTemplate(subject, textBody, htmlBody);
            } catch (EmailException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new EmailException("Failed to template email", exception);
            }
        }
    }
}
