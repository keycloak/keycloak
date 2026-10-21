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

package org.keycloak.forms.login;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface LoginFormsProvider extends Provider {

    String UPDATE_PROFILE_CONTEXT_ATTR = "updateProfileCtx";

    String IDENTITY_PROVIDER_BROKER_CONTEXT = "identityProviderBrokerCtx";

    String USERNAME_HIDDEN = "usernameHidden";

    String REGISTRATION_DISABLED = "registrationDisabled";


    /**
     * Adds a script to the html header
     *
     * @param scriptUrl
     */
    void addScript(String scriptUrl);

    Response createResponse(UserModel.RequiredAction action);

    Response createForm(String form);

    String getMessage(String message, Object... parameters);

    Response createLoginUsernamePassword();

    Response createLoginUsername();

    Response createLoginPassword();

    Response  createOtpReset();

    Response createPasswordReset();

    Response createLoginTotp();

    Response createLoginRecoveryAuthnCode();

    Response createLoginWebAuthn();

    Response createRegistration();

    Response createInfoPage();

    Response createUpdateProfilePage();

    Response createIdpLinkConfirmLinkPage();

    Response createIdpLinkConfirmOverrideLinkPage();

    Response createIdpLinkEmailPage();

    Response createLoginExpiredPage();

    Response createErrorPage(Response.Status status);

    Response createWebAuthnErrorPage();

    Response createOAuthGrant();

    Response createSelectAuthenticator();

    Response createOAuth2DeviceVerifyUserCodePage();

    Response createCode();

    Response createX509ConfirmPage();

    Response createSamlPostForm();

    Response createFrontChannelLogoutPage();

    Response createLogoutConfirmPage();

    LoginFormsProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession);

    LoginFormsProvider setClientSessionCode(String accessCode);

    LoginFormsProvider setAccessRequest(List<AuthorizationDetails> clientScopesRequested);

    /**
     * Set one global error message.
     * 
     * @param message key of message
     * @param parameters to be formatted into message
     */
    LoginFormsProvider setError(String message, Object ... parameters);
    
    /**
     * Set multiple error messages.
     * 
     * @param messages to be set
     */
    LoginFormsProvider setErrors(List<FormMessage> messages);

    LoginFormsProvider addError(FormMessage errorMessage);

    /**
     * Add a success message to the form
     *
     * @param errorMessage
     * @return
     */
    LoginFormsProvider addSuccess(FormMessage errorMessage);

    LoginFormsProvider setSuccess(String message, Object ... parameters);

    LoginFormsProvider setInfo(String message, Object ... parameters);

    LoginFormsProvider setMessage(MessageType type, String message, Object... parameters);

    /**
     * Used when authenticationSession was already removed for this browser session and hence we don't have any
     * authenticationSession or user data. Would just repeat previous info/error page after language is changed
     */
    LoginFormsProvider setDetachedAuthSession();

    LoginFormsProvider setUser(UserModel user);

    LoginFormsProvider setResponseHeader(String headerName, String headerValue);

    LoginFormsProvider setFormData(MultivaluedMap<String, String> formData);

    LoginFormsProvider setAttribute(String name, Object value);

    LoginFormsProvider setStatus(Response.Status status);

    LoginFormsProvider setActionUri(URI requestUri);

    LoginFormsProvider setExecution(String execution);

    LoginFormsProvider setAuthContext(AuthenticationFlowContext context);

    LoginFormsProvider setAttributeMapper(Function<Map<String, Object>, Map<String, Object>> configurer);
}
