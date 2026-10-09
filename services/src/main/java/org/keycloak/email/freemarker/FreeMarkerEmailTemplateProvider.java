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
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ContextNotActiveException;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
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
import org.keycloak.models.KeycloakSessionTask;
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
    private static final String ASYNC_DELIVERY_EXECUTOR = "email-delivery";
    /**
     * Emails waiting to be sent in the background, shared by all sessions. When too many are waiting, for example because
     * the mail server is slow, further emails are sent right away instead, as without sending in the background.
     */
    private static final int MAX_PENDING_ASYNC_DELIVERIES = 1000;
    private static final AtomicInteger pendingAsyncDeliveries = new AtomicInteger();
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
    /**
     * When set, emails are sent in the background, see {@link #setAsyncDelivery(AsyncDeliveryCallback)}.
     */
    protected AsyncDeliveryCallback asyncDeliveryCallback;

    public FreeMarkerEmailTemplateProvider(KeycloakSession session) {
        this.session = session;
        this.freeMarker = session.getProvider(FreeMarkerProvider.class);
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

    @Override
    public boolean setAsyncDelivery(AsyncDeliveryCallback callback) {
        this.asyncDeliveryCallback = callback;
        return callback != null;
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

        send("passwordResetSubject", "password-reset.ftl", attributes);
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
        AsyncDeliveryCallback callback = asyncDeliveryCallback;
        if (callback == null) {
            sendNow(session, config, user, subject, textBody, htmlBody, address);
        } else if (reservePendingAsyncDelivery()) {
            sendAsync(callback, config, subject, textBody, htmlBody, address);
        } else {
            // Too many emails are waiting to be sent, so send this one right away, as without sending in the background
            try {
                sendNow(session, config, user, subject, textBody, htmlBody, address);
            } catch (EmailException e) {
                callback.onFailed(session, e);
                return;
            }
            callback.onSent(session);
        }
    }

    private static void sendNow(KeycloakSession session, Map<String, String> config, UserModel user, String subject,
            String textBody, String htmlBody, String address) throws EmailException {
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

    private void sendAsync(AsyncDeliveryCallback callback, Map<String, String> config, String subject, String textBody,
            String htmlBody, String address) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor(ASYNC_DELIVERY_EXECUTOR);
        Map<String, String> smtpConfig = new HashMap<>(config);
        String realmId = realm.getId();
        String userId = user == null ? null : user.getId();

        Runnable delivery = () -> runInRealm(sessionFactory, realmId, deliverySession -> {
            EmailException failure = null;
            try {
                UserModel recipient = null;
                if (address == null) {
                    recipient = deliverySession.users().getUserById(deliverySession.getContext().getRealm(), userId);
                    if (recipient == null) {
                        throw new EmailException("The recipient of the email no longer exists");
                    }
                }
                sendNow(deliverySession, smtpConfig, recipient, subject, textBody, htmlBody, address);
            } catch (EmailException e) {
                failure = e;
            } catch (RuntimeException e) {
                // For example, loading the recipient failed
                failure = new EmailException("Failed to send the email", e);
            }
            if (failure == null) {
                callback.onSent(deliverySession);
            } else {
                callback.onFailed(deliverySession, failure);
            }
        });

        // Send only after the data referenced by the email is committed, and not at all when the transaction is rolled back
        try {
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    try {
                        executor.execute(() -> {
                            try {
                                delivery.run();
                            } catch (RuntimeException e) {
                                log.error("Failed to send email", e);
                            } finally {
                                releasePendingAsyncDelivery();
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        // The server is shutting down
                        releasePendingAsyncDelivery();
                        reportNotQueued(sessionFactory, realmId, callback, new EmailException("The email could not be queued to be sent", e));
                    }
                }

                @Override
                protected void rollbackImpl() {
                    releasePendingAsyncDelivery();
                }
            });
        } catch (RuntimeException e) {
            releasePendingAsyncDelivery();
            throw e;
        }
    }

    private static boolean reservePendingAsyncDelivery() {
        if (pendingAsyncDeliveries.incrementAndGet() > MAX_PENDING_ASYNC_DELIVERIES) {
            pendingAsyncDeliveries.decrementAndGet();
            return false;
        }
        return true;
    }

    private static void releasePendingAsyncDelivery() {
        pendingAsyncDeliveries.decrementAndGet();
    }

    private static void runInRealm(KeycloakSessionFactory sessionFactory, String realmId, KeycloakSessionTask task) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            session.getContext().setRealm(session.realms().getRealm(realmId));
            task.run(session);
        });
    }

    private static void reportNotQueued(KeycloakSessionFactory sessionFactory, String realmId, AsyncDeliveryCallback callback,
            EmailException e) {
        // The request is already committed at this point, so a failure here must not fail it
        try {
            runInRealm(sessionFactory, realmId, session -> callback.onFailed(session, e));
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
