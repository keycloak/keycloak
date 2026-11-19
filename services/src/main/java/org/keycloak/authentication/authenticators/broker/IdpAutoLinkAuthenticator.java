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

package org.keycloak.authentication.authenticators.broker;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.Booleans;

import org.jboss.logging.Logger;


/**
 * @author <a href="mailto:Ryan.Slominski@gmail.com">Ryan Slominski</a>
 */
public class IdpAutoLinkAuthenticator extends AbstractIdpAuthenticator {

    private static Logger logger = Logger.getLogger(IdpAutoLinkAuthenticator.class);

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

        // Check if email was changed on Review Profile Page
        String emailChanged = context.getAuthenticationSession().getAuthNote(AbstractIdentityProvider.UPDATE_PROFILE_EMAIL_CHANGED);
        boolean emailWasEdited = "true".equals(emailChanged);

        // Check if username was changed on Review Profile Page
        String usernameChanged = context.getAuthenticationSession().getAuthNote(AbstractIdentityProvider.UPDATE_PROFILE_USERNAME_CHANGED);
        boolean usernameWasEdited = "true".equals(usernameChanged);

        // Don't auto-link when email or username edited on Review Profile.
        if (emailWasEdited || usernameWasEdited) {
            logger.debugf("Auto-link blocked: email or username was edited on Review Profile Page");
            context.attempted();
            return;
        }

        String existingUserInfoJson = context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO);
        if (existingUserInfoJson != null) {
            ExistingUserInfo existingUserInfo = ExistingUserInfo.deserialize(existingUserInfoJson);

            if (UserModel.EMAIL.equals(existingUserInfo.getDuplicateAttributeName())) {
                IdentityProviderModel idpConfig = brokerContext.getIdpConfig();

                // Email must match the broker email
                if (brokerContext.getEmail() == null || !brokerContext.getEmail().equalsIgnoreCase(existingUserInfo.getDuplicateAttributeValue())) {
                    logger.debugf("Auto-link blocked: broker email '%s' does not match existing user email '%s'",
                            brokerContext.getEmail(), existingUserInfo.getDuplicateAttributeValue());
                    context.attempted();
                    return;
                }

                // Email must be verified or trusted
                if (!Booleans.isTrue(idpConfig.isTrustEmail()) && !brokerContext.isEmailVerified()) {
                    logger.debugf("Auto-link blocked: email '%s' is not verified by IdP '%s'",
                            brokerContext.getEmail(), idpConfig.getAlias());
                    context.attempted();
                    return;
                }
            }
        }

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        UserModel existingUser = getExistingUser(session, realm, authSession);

        logger.debugf("User '%s' is set to authentication context when link with identity provider '%s' . Identity provider username is '%s' ", existingUser.getUsername(),
                brokerContext.getIdpConfig().getAlias(), brokerContext.getUsername());

        context.setUser(existingUser);
        context.success();
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        authenticateImpl(context, serializedCtx, brokerContext);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

}
