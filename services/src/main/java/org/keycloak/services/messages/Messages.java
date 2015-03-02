/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.messages;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Messages {

    public static final String INVALID_USER = "invalidUserMessage";

    public static final String INVALID_EMAIL = "invalidEmailMessage";

    public static final String ACCOUNT_DISABLED = "accountDisabledMessage";

    public static final String ACCOUNT_TEMPORARILY_DISABLED = "accountTemporarilyDisabledMessage";

    public static final String EXPIRED_CODE = "expiredCodeMessage";

    public static final String MISSING_FIRST_NAME = "missingFirstNameMessage";

    public static final String MISSING_LAST_NAME = "missingLastNameMessage";

    public static final String MISSING_EMAIL = "missingEmailMessage";

    public static final String MISSING_USERNAME = "missingUsernameMessage";

    public static final String MISSING_PASSWORD = "missingPasswordMessage";

    public static final String MISSING_TOTP = "missingTotpMessage";

    public static final String NOTMATCH_PASSWORD = "notMatchPasswordMessage";

    public static final String INVALID_PASSWORD_EXISTING = "invalidPasswordExistingMessage";

    public static final String INVALID_PASSWORD_CONFIRM = "invalidPasswordConfirmMessage";

    public static final String INVALID_TOTP = "invalidTotpMessage";

    public static final String USERNAME_EXISTS = "usernameExistsMessage";

    public static final String EMAIL_EXISTS = "emailExistsMessage";

    public static final String FEDERATED_IDENTITY_EMAIL_EXISTS = "federatedIdentityEmailExistsMessage";

    public static final String FEDERATED_IDENTITY_USERNAME_EXISTS = "federatedIdentityUsernameExistsMessage";



    public static final String READ_ONLY_USER = "readOnlyUser";

    public static final String READ_ONLY_PASSWORD = "readOnlyPassword";

    public static final String ACTION_WARN_TOTP = "actionTotpWarning";

    public static final String ACTION_WARN_PROFILE = "actionProfileWarning";

    public static final String ACTION_WARN_PASSWD = "actionPasswordWarning";

    public static final String ACTION_WARN_EMAIL = "actionEmailWarning";

    public static final String MISSING_IDENTITY_PROVIDER = "missingIdentityProvider";

    public static final String INVALID_FEDERATED_IDENTITY_ACTION = "invalidFederatedIdentityAction";

    public static final String IDENTITY_PROVIDER_NOT_FOUND = "identityProviderNotFound";

    public static final String FEDERATED_IDENTITY_NOT_ACTIVE = "federatedIdentityLinkNotActive";

    public static final String FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER = "federatedIdentityRemovingLastProvider";

    public static final String IDENTITY_PROVIDER_REDIRECT_ERROR = "identityProviderRedirectError";

    public static final String IDENTITY_PROVIDER_REMOVED = "identityProviderRemoved";

    public static final String IDENTITY_PROVIDER_UNEXPECTED_ERROR = "identityProviderUnexpectedError";

    public static final String IDENTITY_PROVIDER_NO_TOKEN = "identityProviderNoToken";

    public static final String ERROR = "error";

    public static final String REALM_SUPPORTS_NO_CREDENTIALS = "realmSupportsNoCredentials";

    public static final String IDENTITY_PROVIDER_NOT_UNIQUE = "identityProviderNotUnique";

    public static final String NO_ACCESS = "noAccess";

    public static final String EMAIL_SENT = "emailSent";

    public static final String EMAIL_SENT_ERROR = "emailSendError";

    public static final String FAILED_TO_PROCESS_RESPONSE = "failedToProcessResponse";

    public static final String HTTPS_REQUIRED = "httpsRequired";

    public static final String REALM_NOT_ENABLED = "realmNotEnabled";

    public static final String INVALID_REQUEST = "invalidRequest";

    public static final String INVALID_REQUESTER = "invalidRequester";

    public static final String UNKNOWN_LOGIN_REQUESTER = "unknownLoginRequester";

    public static final String LOGIN_REQUESTER_NOT_ENABLED = "loginRequesterNotEnabled";

    public static final String BEARER_ONLY = "bearerOnly";

    public static final String DIRECT_GRANTS_ONLY = "directGrantsOnly";

    public static final String INVALID_REDIRECT_URI = "invalidRedirectUri";

    public static final String UNSUPPORTED_NAME_ID_FORMAT = "unsupportedNameIdFormat";

    public static final String REGISTRATION_NOT_ALLOWED = "registrationNotAllowed";

    public static final String PERMISSION_NOT_APPROVED = "permissionNotApproved";

    public static final String NO_RELAY_STATE_IN_RESPONSE = "noRelayStateInResponse";

    public static final String IDENTITY_PROVIDER_ALREADY_LINKED = "identityProviderAlreadyLinked";

    public static final String USER_DISABLED = "userDisabled";
    
    public static final String INSUFFICIENT_PERMISSION = "insufficientPermission";

    public static final String COULD_NOT_PROCEED_WITH_AUTHENTICATION_REQUEST = "couldNotProceedWithAuthenticationRequest";

    public static final String COULD_NOT_OBTAIN_TOKEN = "couldNotObtainToken";

    public static final String UNEXPECTED_ERROR_RETRIEVING_TOKEN = "unexpectedErrorRetrievingToken";

    public static final String IDENTITY_PROVIDER_AUTHENTICATION_FAILED = "identityProviderAuthenticationFailed";

    public static final String UNEXPECTED_ERROR_HANDLING_RESPONSE = "unexpectedErrorHandlingResponse";

    public static final String COULD_NOT_SEND_AUTHENTICATION_REQUEST = "couldNotSendAuthenticationRequest";

    public static final String UNEXPECTED_ERROR_HANDLING_REQUEST = "unexpectedErrorHandlingRequest";

    public static final String INVALID_ACCESS_CODE = "invalidAccessCode";

    public static final String SESSION_NOT_ACTIVE = "sessionNotActive";

    public static final String UNKNOWN_CODE = "unknownCode";

    public static final String INVALID_CODE = "invalidCode";


}
