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

package org.keycloak.email.freemarker;

import java.io.IOException;
import java.text.Bidi;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ContextNotActiveException;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailDeliveryListener;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.RenderedEmail;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.LinkExpirationFormatterMethod;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.theme.freemarker.FreeMarkerProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailTemplateProvider implements EmailTemplateProvider {

    private static final Logger log = LoggerFactory.getLogger(FreeMarkerEmailTemplateProvider.class);
    private static final String PASSWORD_RESET_SUBJECT = "passwordResetSubject";
    private static final String PASSWORD_RESET_TEMPLATE = "password-reset.ftl";
    private static final String BACKGROUND_EXECUTOR = "email";
    private static final int DEFAULT_BACKGROUND_THREADS = 4;
    protected KeycloakSession session;
    /**
     * authenticationSession can be null for some email sendings, it is filled only for email sendings performed as part of the authentication session (email verification, password reset, broker link
     * etc.)!
     */
    protected AuthenticationSessionModel authenticationSession;
    protected FreeMarkerProvider freeMarker;
    protected RealmModel realm;
    protected UserModel user;
    protected final Map<String, Object> attributes = new HashMap<>();
    private final AtomicInteger backgroundDeliveries;

    public FreeMarkerEmailTemplateProvider(KeycloakSession session) {
        this(session, null);
    }

    /**
     * @param backgroundDeliveries the factory's counter of emails being sent in the background, or {@code null} to
     * always send right away
     */
    protected FreeMarkerEmailTemplateProvider(KeycloakSession session, AtomicInteger backgroundDeliveries) {
        this.session = session;
        this.freeMarker = session.getProvider(FreeMarkerProvider.class);
        this.backgroundDeliveries = backgroundDeliveries;
    }

    @Override
    public EmailTemplateProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public EmailTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public EmailTemplateProvider setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
        return this;
    }

    protected String getRealmName() {
        if (realm.getDisplayName() != null) {
            return realm.getDisplayName();
        } else {
            return ObjectUtil.capitalize(realm.getName());
        }
    }

    @Override
    public void sendEvent(Event event) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("event", new EventBean(event));

        send(toCamelCase(event.getType()) + "Subject", "event-" + event.getType().toString().toLowerCase() + ".ftl", attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        send(PASSWORD_RESET_SUBJECT, PASSWORD_RESET_TEMPLATE, attributes);
    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {
        setRealm(session.getContext().getRealm());
        setUser(user);

        Map<String, Object> attributes = new HashMap<>(this.attributes);

        EmailTemplate email = processTemplate("emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
        send(config, email.getSubject(), email.getTextBody(), email.getHtmlBody());
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get(IDENTITY_PROVIDER_BROKER_CONTEXT);
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        String idpDisplayName = brokerContext.getIdpConfig().getDisplayName();
        if (ObjectUtil.isBlank(idpDisplayName)) {
            idpDisplayName = ObjectUtil.capitalize(idpAlias);
        }

        attributes.put("identityProviderContext", brokerContext);
        attributes.put("identityProviderAlias", idpAlias);
        attributes.put("identityProviderDisplayName", idpDisplayName);

        List<Object> subjectAttrs = Collections.singletonList(idpDisplayName);
        send("identityProviderLinkSubject", subjectAttrs, "identity-provider-link.ftl", attributes);
    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        send("executeActionsSubject", "executeActions.ftl", attributes);
    }

    @Override
    public void sendVerifiableCredentialOffer(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        send("verifiableCredentialOfferSubject", "verifiable-credential-offer.ftl", attributes);
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        send("emailVerificationSubject", "email-verification.ftl", attributes);
    }

    @Override
    public void sendOrgInviteEmail(OrganizationModel organization, String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("organization", organization);
        if (user.getFirstName() != null && user.getLastName() != null) {
            attributes.put("firstName", user.getFirstName());
            attributes.put("lastName", user.getLastName());
        }
        send("orgInviteSubject", List.of(organization.getName()), "org-invite.ftl", attributes);
    }

    @Override
    public void sendEmailUpdateConfirmation(String link, long expirationInMinutes, String newEmail) throws EmailException {
        if (newEmail == null) {
            throw new IllegalArgumentException("The new email is mandatory");
        }

        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("newEmail", newEmail);

        send("emailUpdateConfirmationSubject", Collections.emptyList(), "email-update-confirmation.ftl", attributes, newEmail);
    }

    /**
     * Add link info into template attributes.
     *
     * @param link to add
     * @param expirationInMinutes to add
     * @param attributes to add link info into
     */
    protected void addLinkInfoIntoAttributes(String link, long expirationInMinutes, Map<String, Object> attributes) throws EmailException {
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);
        try {
            Locale locale = session.getContext().resolveLocale(user, Boolean.parseBoolean(String.valueOf(attributes.get(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER))));
            attributes.put("linkExpirationFormatter", new LinkExpirationFormatterMethod(getTheme().getMessages(locale), locale));
        } catch (IOException e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        send(subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes);
    }

    protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
        try {
            Locale locale = session.getContext().resolveLocale(user, Boolean.parseBoolean(String.valueOf(attributes.get(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER))));
            attributes.put("locale", locale);

            Theme theme = getTheme();
            Properties messages = theme.getEnhancedMessages(realm, locale);

            String currentLanguageTag = locale.getLanguage();
            String currentLanguage = messages.getProperty("locale_" + currentLanguageTag, currentLanguageTag);
            boolean isLtr = new Bidi(currentLanguage, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isLeftToRight();
            attributes.put("ltr", isLtr);

            attributes.put("msg", new MessageFormatterMethod(locale, messages));

            attributes.put("properties", theme.getProperties());
            attributes.put("realmName", getRealmName());
            attributes.put("user", new ProfileBean(user, session));

            try {
                KeycloakUriInfo uriInfo = session.getContext().getUri();
                attributes.put("url", new UrlBean(realm, theme, uriInfo.getBaseUri(), null));
            } catch (ContextNotActiveException e) {
                log.debug("No active request, can't make url attribute available to the template");
                // ignore when running without an active request context such as sending emails from an scheduled task
            }

            String subject = new MessageFormat(messages.getProperty(subjectKey, subjectKey), locale).format(subjectAttributes.toArray());
            attributes.put("subject", subject);
            String textTemplate = String.format("text/%s", template);
            String textBody;
            try {
                textBody = freeMarker.processTemplate(attributes, textTemplate, theme);
            } catch (final FreeMarkerException e) {
                throw new EmailException("Failed to template plain text email.", e);
            }
            String htmlTemplate = String.format("html/%s", template);
            String htmlBody;
            try {
                htmlBody = freeMarker.processTemplate(attributes, htmlTemplate, theme);
            } catch (final FreeMarkerException e) {
                throw new EmailException("Failed to template html email.", e);
            }

            return new EmailTemplate(subject, textBody, htmlBody);
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.EMAIL);
    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        send(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes, null);
    }

    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes, String destinationEmail) throws EmailException {
        send(subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes, destinationEmail);
    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate,
        Map<String, Object> bodyAttributes, String address) throws EmailException {
        try {
            EmailTemplate email = processTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
            send(email.getSubject(), email.getTextBody(), email.getHtmlBody(), address);
        } catch (EmailException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    protected void send(String subject, String textBody, String htmlBody, String address) throws EmailException {
        send(realm.getSmtpConfig(), subject, textBody, htmlBody, address);
    }

    protected void send(Map<String, String> config, String subject, String textBody, String htmlBody) throws EmailException {
        send(config, subject, textBody, htmlBody, null);
    }

    protected void send(Map<String, String> config, String subject, String textBody, String htmlBody, String address) throws EmailException {
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
        if (emailSender == null) {
            throw new EmailException("Email sender provider is disabled or not configured");
        }
        if (address == null) {
            emailSender.send(config, user, subject, textBody, htmlBody);
        } else {
            emailSender.send(config, address, subject, textBody, htmlBody);
        }
    }

    @Override
    public RenderedEmail renderPasswordReset(String link, long expirationInMinutes) throws EmailException {
        if (backgroundDeliveries == null || !supportsBackgroundDelivery()) {
            return null;
        }
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        EmailTemplate email = processTemplate(PASSWORD_RESET_SUBJECT, Collections.emptyList(), PASSWORD_RESET_TEMPLATE, attributes);
        return new RenderedEmail(email.getSubject(), email.getTextBody(), email.getHtmlBody());
    }

    @Override
    public void sendPasswordReset(RenderedEmail email, int expiresAt, EmailDeliveryListener onFailed) throws EmailException {
        if (backgroundDeliveries == null || !supportsBackgroundDelivery()) {
            EmailTemplateProvider.super.sendPasswordReset(email, expiresAt, onFailed);
            return;
        }
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor(BACKGROUND_EXECUTOR);
        // one email per thread at most, so nothing waits in a queue. Beyond that, send right away as before
        if (backgroundDeliveries.incrementAndGet() > backgroundThreads(executor)) {
            backgroundDeliveries.decrementAndGet();
            send(realm.getSmtpConfig(), email.subject(), email.textBody(), email.htmlBody(), null);
            return;
        }
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            String realmId = realm.getId();
            String userId = user.getId();
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    try {
                        executor.execute(() -> {
                            try {
                                deliver(sessionFactory, realmId, userId, email, expiresAt, onFailed);
                            } catch (RuntimeException e) {
                                log.error("Failed to send email", e);
                            } finally {
                                backgroundDeliveries.decrementAndGet();
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        backgroundDeliveries.decrementAndGet();
                        reportNotSent(sessionFactory, realmId, onFailed, new EmailException("The email was not sent, as the server is shutting down", e));
                    }
                }

                @Override
                protected void rollbackImpl() {
                    backgroundDeliveries.decrementAndGet();
                }
            });
        } catch (RuntimeException e) {
            backgroundDeliveries.decrementAndGet();
            throw e;
        }
    }

    /**
     * Subclasses may change how emails are sent, so by default they send right away. Return true to send in the
     * background; the factory then has to pass its counter to the constructor.
     */
    protected boolean supportsBackgroundDelivery() {
        return getClass() == FreeMarkerEmailTemplateProvider.class;
    }

    private static int backgroundThreads(ExecutorService executor) {
        return executor instanceof ThreadPoolExecutor pool ? Math.max(1, pool.getCorePoolSize()) : DEFAULT_BACKGROUND_THREADS;
    }

    private static void deliver(KeycloakSessionFactory sessionFactory, String realmId, String userId, RenderedEmail email,
            int expiresAt, EmailDeliveryListener onFailed) {
        // the transaction stays open during the send, as it did in the request before
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                log.warn("Email not sent, as its realm no longer exists");
                return;
            }
            session.getContext().setRealm(realm);
            try {
                if (expiresAt > 0 && Time.currentTime() >= expiresAt) {
                    throw new EmailException("The email expired before it could be sent");
                }
                UserModel user = session.users().getUserById(realm, userId);
                if (user == null || !user.isEnabled()) {
                    throw new EmailException("The user no longer exists or is disabled");
                }
                EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
                if (emailSender == null) {
                    throw new EmailException("Email sender provider is disabled or not configured");
                }
                emailSender.send(realm.getSmtpConfig(), user, email.subject(), email.textBody(), email.htmlBody());
            } catch (EmailException e) {
                onFailed.onFailed(session, e);
            } catch (RuntimeException e) {
                onFailed.onFailed(session, new EmailException("Failed to send the email", e));
            }
        });
    }

    // must not throw, as the request already finished
    private static void reportNotSent(KeycloakSessionFactory sessionFactory, String realmId, EmailDeliveryListener onFailed,
            EmailException e) {
        try {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
                session.getContext().setRealm(session.realms().getRealm(realmId));
                onFailed.onFailed(session, e);
            });
        } catch (RuntimeException re) {
            log.error("Failed to report an email that was not sent", re);
        }
    }

    @Override
    public void close() {
    }

    protected String toCamelCase(EventType event) {
        StringBuilder sb = new StringBuilder("event");
        for (String s : event.name().toLowerCase().split("_")) {
            sb.append(ObjectUtil.capitalize(s));
        }
        return sb.toString();
    }

    protected static class EmailTemplate {

        private String subject;
        private String textBody;
        private String htmlBody;

        public EmailTemplate(String subject, String textBody, String htmlBody) {
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
        }

        public String getSubject() {
            return subject;
        }

        public String getTextBody() {
            return textBody;
        }

        public String getHtmlBody() {
            return htmlBody;
        }
    }

}
