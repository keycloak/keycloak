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

package org.keycloak.email;

import java.util.List;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EmailTemplateProvider extends Provider {

    String IDENTITY_PROVIDER_BROKER_CONTEXT = "identityProviderBrokerCtx";
    
    EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession);

    EmailTemplateProvider setRealm(RealmModel realm);

    EmailTemplateProvider setUser(UserModel user);

    EmailTemplateProvider setAttribute(String name, Object value);

    /**
     * Builds the reset password email without sending it. Send it with {@link #queue}.
     *
     * @return the email, or {@code null} if this provider can only send emails right away
     */
    default RenderedEmail renderPasswordReset(String link, long expirationInMinutes) throws EmailException {
        return null;
    }

    /**
     * Sends the email to the user set on this provider, in the background once the transaction commits. Nothing is
     * sent if it rolls back.
     *
     * @param expiresAt time in seconds after which the email is not sent anymore, or {@code 0} for no limit
     * @param listener called if the email is not sent
     * @return {@code false} if too many emails are waiting, then this one is not sent
     */
    default boolean queue(RenderedEmail email, int expiresAt, EmailDeliveryListener listener) {
        throw new UnsupportedOperationException("Sending emails in the background is not supported by " + getClass().getName());
    }

    void sendEvent(Event event) throws EmailException;

    /**
     * Reset password sent from forgot password link on login
     *
     * @param link
     * @param expirationInMinutes
     * @throws EmailException
     */
    void sendPasswordReset(String link, long expirationInMinutes) throws EmailException;

    /**
     * Test SMTP connection with current logged in user
     *
     * @param config SMTP server configuration
     * @param user SMTP recipient
     * @throws EmailException
     */
    void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException;

    /**
     * Send to confirm that user wants to link his account with identity broker link
     */
    void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException;

    /**
     * Change password email requested by admin
     *
     * @param link
     * @param expirationInMinutes
     * @throws EmailException
     */
    void sendExecuteActions(String link, long expirationInMinutes) throws EmailException;

    void sendVerifiableCredentialOffer(String link, long expirationInMinutes) throws EmailException;

    void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException;

    void sendOrgInviteEmail(OrganizationModel organization, String link, long expirationInMinutes) throws EmailException;

    void sendEmailUpdateConfirmation(String link, long expirationInMinutes, String address) throws EmailException;

    /**
     * Send formatted email
     *
     * @param subjectFormatKey message property that will be used to format email subject
     * @param bodyTemplate freemarker template file
     * @param bodyAttributes attributes used to fill template
     * @throws EmailException
     */
    void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException;

    /**
     * Send formatted email
     *
     * @param subjectFormatKey message property that will be used to format email subject
     * @param subjectAttributes attributes used to fill subject format message
     * @param bodyTemplate freemarker template file
     * @param bodyAttributes attributes used to fill template
     * @throws EmailException
     */
    void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException;
    /**
     * Send formatted email to a specific address, ignoring the user's email
     *
     * @param subjectFormatKey message property that will be used to format email subject
     * @param bodyTemplate freemarker template file
     * @param bodyAttributes attributes used to fill template
     * @param destinationEmail the email address to send the email to
     * @throws EmailException
     */
    void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes, String destinationEmail) throws EmailException;

    /**
     * Send formatted email to a specific address, ignoring the user's email
     *
     * @param subjectFormatKey message property that will be used to format email subject
     * @param subjectAttributes attributes used to fill subject format message
     * @param bodyTemplate freemarker template file
     * @param bodyAttributes attributes used to fill template
     * @param destinationEmail the email address to send the email to
     * @throws EmailException
     */
    void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes, String destinationEmail) throws EmailException;
}
