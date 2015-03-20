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

    public static final String CONFIGURE_TOTP = "configureTotpMessage";

    public static final String UPDATE_PROFILE = "updateProfileMessage";

    public static final String UPDATE_PASSWORD = "updatePasswordMessage";

    public static final String VERIFY_EMAIL = "verifyEmailMessage";

    public static final String EMAIL_VERIFIED = "emailVerifiedMessage";

    public static final String EMAIL_SENT = "emailSentMessage";

    public static final String EMAIL_SENT_ERROR = "emailSendErrorMessage";

    public static final String ACCOUNT_UPDATED = "accountUpdatedMessage";

    public static final String ACCOUNT_PASSWORD_UPDATED = "accountPasswordUpdatedMessage";

    public static final String NO_ACCESS = "noAccessMessage";

    public static final String FAILED_TO_PROCESS_RESPONSE = "failedToProcessResponseMessage";

    public static final String HTTPS_REQUIRED = "httpsRequiredMessage";

    public static final String REALM_NOT_ENABLED = "realmNotEnabledMessage";

    public static final String INVALID_REQUEST = "invalidRequestMessage";

    public static final String INVALID_REQUESTER = "invalidRequesterMessage";

    public static final String UNKNOWN_LOGIN_REQUESTER = "unknownLoginRequesterMessage";

    public static final String LOGIN_REQUESTER_NOT_ENABLED = "loginRequesterNotEnabledMessage";

    public static final String BEARER_ONLY = "bearerOnlyMessage";

    public static final String DIRECT_GRANTS_ONLY = "directGrantsOnlyMessage";

    public static final String INVALID_REDIRECT_URI = "invalidRedirectUriMessage";

    public static final String UNSUPPORTED_NAME_ID_FORMAT = "unsupportedNameIdFormatMessage";

    public static final String REGISTRATION_NOT_ALLOWED = "registrationNotAllowedMessage";

    public static final String PERMISSION_NOT_APPROVED = "permissionNotApprovedMessage";

    public static final String NO_RELAY_STATE_IN_RESPONSE = "noRelayStateInResponseMessage";

    public static final String IDENTITY_PROVIDER_ALREADY_LINKED = "identityProviderAlreadyLinkedMessage";

    public static final String INSUFFICIENT_PERMISSION = "insufficientPermissionMessage";

    public static final String COULD_NOT_PROCEED_WITH_AUTHENTICATION_REQUEST = "couldNotProceedWithAuthenticationRequestMessage";

    public static final String COULD_NOT_OBTAIN_TOKEN = "couldNotObtainTokenMessage";

    public static final String UNEXPECTED_ERROR_RETRIEVING_TOKEN = "unexpectedErrorRetrievingTokenMessage";

    public static final String IDENTITY_PROVIDER_AUTHENTICATION_FAILED = "identityProviderAuthenticationFailedMessage";

    public static final String UNEXPECTED_ERROR_HANDLING_RESPONSE = "unexpectedErrorHandlingResponseMessage";

    public static final String COULD_NOT_SEND_AUTHENTICATION_REQUEST = "couldNotSendAuthenticationRequestMessage";

    public static final String UNEXPECTED_ERROR_HANDLING_REQUEST = "unexpectedErrorHandlingRequestMessage";

    public static final String INVALID_ACCESS_CODE = "invalidAccessCodeMessage";

    public static final String SESSION_NOT_ACTIVE = "sessionNotActiveMessage";

    public static final String UNKNOWN_CODE = "unknownCodeMessage";

    public static final String INVALID_CODE = "invalidCodeMessage";

    public static final String IDENTITY_PROVIDER_UNEXPECTED_ERROR = "identityProviderUnexpectedErrorMessage";

    public static final String IDENTITY_PROVIDER_NOT_FOUND = "identityProviderNotFoundMessage";

    public static final String IDENTITY_PROVIDER_NOT_UNIQUE = "identityProviderNotUniqueMessage";

    public static final String REALM_SUPPORTS_NO_CREDENTIALS = "realmSupportsNoCredentialsMessage";

    public static final String READ_ONLY_USER = "readOnlyUserMessage";

    public static final String READ_ONLY_PASSWORD = "readOnlyPasswordMessage";

    public static final String SUCCESS_TOTP_REMOVED = "successTotpRemovedMessage";

    public static final String SUCCESS_TOTP = "successTotpMessage";

    public static final String MISSING_IDENTITY_PROVIDER = "missingIdentityProviderMessage";

    public static final String INVALID_FEDERATED_IDENTITY_ACTION = "invalidFederatedIdentityActionMessage";

    public static final String FEDERATED_IDENTITY_NOT_ACTIVE = "federatedIdentityLinkNotActiveMessage";

    public static final String FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER = "federatedIdentityRemovingLastProviderMessage";

    public static final String IDENTITY_PROVIDER_REDIRECT_ERROR = "identityProviderRedirectErrorMessage";

    public static final String IDENTITY_PROVIDER_REMOVED = "identityProviderRemovedMessage";

    public static final String MISSING_PARAMETER = "missingParameterMessage";

    public static final String CLIENT_NOT_FOUND = "clientNotFoundMessage";

    public static final String INVALID_PARAMETER = "invalidParameterMessage";

    public static final String IDENTITY_PROVIDER_LOGIN_FAILURE = "identityProviderLoginFailure";
}
