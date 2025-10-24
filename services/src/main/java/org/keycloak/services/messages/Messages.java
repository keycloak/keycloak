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
package org.keycloak.services.messages;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Messages {

    public static final String DISPLAY_UNSUPPORTED = "displayUnsupported";
    public static final String LOGIN_TIMEOUT = "loginTimeout";

    public static final String REAUTHENTICATE = "reauthenticate";
    public static final String AUTHENTICATE_STRONG = "authenticateStrong";

    public static final String INVALID_USER = "invalidUserMessage";

    public static final String INVALID_USERNAME = "invalidUsernameMessage";

    public static final String INVALID_USERNAME_OR_EMAIL = "invalidUsernameOrEmailMessage";

    public static final String INVALID_PASSWORD = "invalidPasswordMessage";

    public static final String INVALID_EMAIL = "invalidEmailMessage";

    public static final String ORG_NOT_FOUND = "orgNotFoundMessage";

    public static final String ORG_MEMBER_ALREADY = "orgMemberAlready";

    public static final String INVALID_ORG_INVITE = "invalidOrgInviteMessage";

    public static final String ACCOUNT_DISABLED = "accountDisabledMessage";

    public static final String ACCOUNT_TEMPORARILY_DISABLED = "accountTemporarilyDisabledMessage";

    public static final String ACCOUNT_PERMANENTLY_DISABLED = "accountPermanentlyDisabledMessage";

    public static final String ACCOUNT_TEMPORARILY_DISABLED_TOTP = "accountTemporarilyDisabledMessageTotp";

    public static final String ACCOUNT_PERMANENTLY_DISABLED_TOTP = "accountPermanentlyDisabledMessageTotp";

    public static final String EXPIRED_CODE = "expiredCodeMessage";

    public static final String EXPIRED_ACTION = "expiredActionMessage";

    public static final String EXPIRED_ACTION_TOKEN_NO_SESSION = "expiredActionTokenNoSessionMessage";

    public static final String EXPIRED_ACTION_TOKEN_SESSION_EXISTS = "expiredActionTokenSessionExistsMessage";

    public static final String MISSING_FIRST_NAME = "missingFirstNameMessage";

    public static final String MISSING_LAST_NAME = "missingLastNameMessage";

    public static final String MISSING_EMAIL = "missingEmailMessage";

    public static final String MISSING_USERNAME = "missingUsernameMessage";

    public static final String UPDATE_READ_ONLY_ATTRIBUTES_REJECTED = "updateReadOnlyAttributesRejectedMessage";

    public static final String MISSING_PASSWORD = "missingPasswordMessage";

    public static final String MISSING_TOTP = "missingTotpMessage";

    public static final String RESET_OTP_MISSING_ID_ERROR = "error-reset-otp-missing-id";

    public static final String MISSING_TOTP_DEVICE_NAME = "missingTotpDeviceNameMessage";

    public static final String COOKIE_NOT_FOUND = "cookieNotFoundMessage";

    public static final String NOTMATCH_PASSWORD = "notMatchPasswordMessage";

    public static final String INVALID_PASSWORD_EXISTING = "invalidPasswordExistingMessage";

    public static final String INVALID_PASSWORD_CONFIRM = "invalidPasswordConfirmMessage";

    public static final String INVALID_TOTP = "invalidTotpMessage";

    public static final String USERNAME_EXISTS = "usernameExistsMessage";
    public static final String RECAPTCHA_FAILED = "recaptchaFailed";
    public static final String RECAPTCHA_NOT_CONFIGURED = "recaptchaNotConfigured";

    public static final String EMAIL_EXISTS = "emailExistsMessage";
    public static final String EMAIL_VERIFICATION_PENDING = "emailVerificationPending";

    public static final String FEDERATED_IDENTITY_EXISTS = "federatedIdentityExistsMessage";

    public static final String FEDERATED_IDENTITY_UNAVAILABLE = "federatedIdentityUnavailableMessage";

    public static final String FEDERATED_IDENTITY_CONFIRM_LINK_MESSAGE = "federatedIdentityConfirmLinkMessage";

    public static final String FEDERATED_IDENTITY_CONFIRM_OVERRIDE_MESSAGE = "federatedIdentityConfirmOverrideMessage";

    public static final String FEDERATED_IDENTITY_CONFIRM_REAUTHENTICATE_MESSAGE = "federatedIdentityConfirmReauthenticateMessage";

    public static final String NESTED_FIRST_BROKER_FLOW_MESSAGE = "nestedFirstBrokerFlowMessage";

    public static final String CONFIGURE_TOTP = "configureTotpMessage";

    public static final String CONFIGURE_BACKUP_CODES = "configureBackupCodesMessage";

    public static final String UPDATE_PROFILE = "updateProfileMessage";

    public static final String RESET_PASSWORD = "resetPasswordMessage";

    public static final String UPDATE_PASSWORD = "updatePasswordMessage";

    public static final String VERIFY_EMAIL = "verifyEmailMessage";

    public static final String COOLDOWN_VERIFICATION_EMAIL = "emailVerifySendCooldown";

    public static final String UPDATE_EMAIL = "updateEmailMessage";

    public static final String LINK_IDP = "linkIdpMessage";

    public static final String EMAIL_VERIFIED = "emailVerifiedMessage";

    public static final String EMAIL_VERIFIED_HEADER = "emailVerifiedMessageHeader";

    public static final String EMAIL_VERIFIED_ALREADY = "emailVerifiedAlreadyMessage";

    public static final String EMAIL_VERIFIED_ALREADY_HEADER = "emailVerifiedAlreadyMessageHeader";

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

    public static final String STANDARD_FLOW_DISABLED = "standardFlowDisabledMessage";

    public static final String IMPLICIT_FLOW_DISABLED = "implicitFlowDisabledMessage";

    public static final String INVALID_REDIRECT_URI = "invalidRedirectUriMessage";

    public static final String UNSUPPORTED_NAME_ID_FORMAT = "unsupportedNameIdFormatMessage";

    public static final String REGISTRATION_NOT_ALLOWED = "registrationNotAllowedMessage";
    public static final String RESET_CREDENTIAL_NOT_ALLOWED = "resetCredentialNotAllowedMessage";

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

    public static final String KERBEROS_NOT_ENABLED="kerberosNotSetUp";

    public static final String UNEXPECTED_ERROR_HANDLING_REQUEST = "unexpectedErrorHandlingRequestMessage";

    public static final String INVALID_ACCESS_CODE = "invalidAccessCodeMessage";

    public static final String SESSION_NOT_ACTIVE = "sessionNotActiveMessage";

    public static final String INVALID_CODE = "invalidCodeMessage";

    public static final String STALE_VERIFY_EMAIL_LINK = "staleEmailVerificationLink";

    public static final String EMAIL_VERIFICATION_CANCELLED = "emailVerificationCancelled";

    public static final String STALE_INVITE_ORG_LINK = "staleInviteOrgLink";

    public static final String IDENTITY_PROVIDER_UNEXPECTED_ERROR = "identityProviderUnexpectedErrorMessage";

    public static final String IDENTITY_PROVIDER_UNMATCHED_ESSENTIAL_CLAIM_ERROR = "federatedIdentityUnmatchedEssentialClaimMessage";

    public static final String IDENTITY_PROVIDER_MISSING_STATE_ERROR = "identityProviderMissingStateMessage";

    public static final String IDENTITY_PROVIDER_MISSING_CODE_OR_ERROR_ERROR = "identityProviderMissingCodeOrErrorMessage";

    public static final String IDENTITY_PROVIDER_INVALID_RESPONSE = "identityProviderInvalidResponseMessage";

    public static final String IDENTITY_PROVIDER_INVALID_SIGNATURE = "identityProviderInvalidSignatureMessage";

    public static final String IDENTITY_PROVIDER_NOT_FOUND = "identityProviderNotFoundMessage";

    public static final String IDENTITY_PROVIDER_LINK_SUCCESS = "identityProviderLinkSuccess";

    public static final String CONFIRM_ACCOUNT_LINKING = "confirmAccountLinking";

    public static final String CONFIRM_ACCOUNT_LINKING_BODY = "confirmAccountLinkingBody";

    public static final String CONFIRM_EMAIL_ADDRESS_VERIFICATION = "confirmEmailAddressVerification";

    public static final String CONFIRM_EMAIL_ADDRESS_VERIFICATION_HEADER = "confirmEmailAddressVerificationHeader";

    public static final String CONFIRM_EXECUTION_OF_ACTIONS = "confirmExecutionOfActions";

    public static final String STALE_CODE = "staleCodeMessage";

    public static final String STALE_CODE_ACCOUNT = "staleCodeAccountMessage";

    public static final String IDENTITY_PROVIDER_NOT_UNIQUE = "identityProviderNotUniqueMessage";

    public static final String REALM_SUPPORTS_NO_CREDENTIALS = "realmSupportsNoCredentialsMessage";

    public static final String CREDENTIAL_SETUP_REQUIRED ="credentialSetupRequired";

    public static final String READ_ONLY_USER = "readOnlyUserMessage";

    public static final String READ_ONLY_USERNAME = "readOnlyUsernameMessage";

    public static final String READ_ONLY_PASSWORD = "readOnlyPasswordMessage";

    public static final String SUCCESS_TOTP_REMOVED = "successTotpRemovedMessage";

    public static final String SUCCESS_TOTP = "successTotpMessage";

    public static final String SUCCESS_GRANT_REVOKED = "successGrantRevokedMessage";

    public static final String MISSING_IDENTITY_PROVIDER = "missingIdentityProviderMessage";

    public static final String INVALID_TOKEN_REQUIRED_ACTIONS = "invalidTokenRequiredActions";

    public static final String INVALID_FEDERATED_IDENTITY_ACTION = "invalidFederatedIdentityActionMessage";

    public static final String FEDERATED_IDENTITY_NOT_ACTIVE = "federatedIdentityLinkNotActiveMessage";

    public static final String FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER = "federatedIdentityRemovingLastProviderMessage";

    public static final String FEDERATED_IDENTITY_BOUND_ORGANIZATION = "federatedIdentityBoundOrganization";

    public static final String IDENTITY_PROVIDER_REDIRECT_ERROR = "identityProviderRedirectErrorMessage";

    public static final String IDENTITY_PROVIDER_REMOVED = "identityProviderRemovedMessage";

    public static final String MISSING_PARAMETER = "missingParameterMessage";

    public static final String CLIENT_NOT_FOUND = "clientNotFoundMessage";

    public static final String CLIENT_DISABLED = "clientDisabledMessage";

    public static final String INVALID_PARAMETER = "invalidParameterMessage";

    public static final String IDENTITY_PROVIDER_LOGIN_FAILURE = "identityProviderLoginFailure";

    public static final String IDENTITY_PROVIDER_LOGOUT_FAILURE = "identityProviderLogoutFailure";

    public static final String INSUFFICIENT_LEVEL_OF_AUTHENTICATION = "insufficientLevelOfAuthentication";

    public static final String SUCCESS_LOGOUT = "successLogout";

    public static final String FAILED_LOGOUT = "failedLogout";

    public static final String CONSENT_DENIED="consentDenied";

    public static final String TERMS_AND_CONDITIONS_DECLINED="termsAndConditionsDeclined";

    public static final String ALREADY_LOGGED_IN="alreadyLoggedIn";

    public static final String DIFFERENT_USER_AUTHENTICATED = "differentUserAuthenticated";

    public static final String BROKER_LINKING_SESSION_EXPIRED = "brokerLinkingSessionExpired";

    public static final String ACR_NOT_FULFILLED = "acrNotFulfilled";

    public static final String PAGE_NOT_FOUND = "pageNotFound";

    public static final String INTERNAL_SERVER_ERROR = "internalServerError";

    public static final String DELEGATION_COMPLETE = "delegationCompleteMessage";
    public static final String DELEGATION_COMPLETE_HEADER = "delegationCompleteHeader";
    public static final String DELEGATION_FAILED = "delegationFailedMessage";
    public static final String DELEGATION_FAILED_HEADER = "delegationFailedHeader";

    public static final String ARTIFACT_RESOLUTION_SERVICE_ERROR = "artifactResolutionServiceError";
    public static final String ARTIFACT_RESOLUTION_SERVICE_INVALID_RESPONSE = "saml.artifactResolutionServiceInvalidResponse";

    // WebAuthn
    public static final String WEBAUTHN_REGISTER_TITLE = "webauthn-registration-title";
    public static final String WEBAUTHN_LOGIN_TITLE = "webauthn-login-title";
    public static final String WEBAUTHN_ERROR_TITLE = "webauthn-error-title";

    // WebAuthn Error
    public static final String WEBAUTHN_ERROR_REGISTRATION = "webauthn-error-registration";
    public static final String WEBAUTHN_ERROR_API_GET = "webauthn-error-api-get";
    public static final String WEBAUTHN_ERROR_DIFFERENT_USER = "webauthn-error-different-user";
    public static final String WEBAUTHN_ERROR_AUTH_VERIFICATION = "webauthn-error-auth-verification";
    public static final String WEBAUTHN_ERROR_REGISTER_VERIFICATION = "webauthn-error-register-verification";
    public static final String WEBAUTHN_ERROR_USER_NOT_FOUND = "webauthn-error-user-not-found";

    // Conditions in Conditional Flow
    public static final String ACCESS_DENIED = "access-denied";

    public static final String ACCESS_DENIED_WHEN_IDP_AUTH = "access-denied-when-idp-auth";

    public static final String DELETE_ACCOUNT_LACK_PRIVILEDGES = "deletingAccountForbidden";
    public static final String DELETE_ACCOUNT_ERROR = "errorDeletingAccount";

    // OAuth 2.0 Device Authorization Grant
    public static final String OAUTH2_DEVICE_AUTHORIZATION_GRANT_DISABLED = "oauth2DeviceAuthorizationGrantDisabledMessage";
    public static final String OAUTH2_DEVICE_INVALID_USER_CODE = "oauth2DeviceInvalidUserCodeMessage";
    public static final String OAUTH2_DEVICE_EXPIRED_USER_CODE = "oauth2DeviceExpiredUserCodeMessage";
    public static final String OAUTH2_DEVICE_VERIFICATION_COMPLETE = "oauth2DeviceVerificationCompleteMessage";
    public static final String OAUTH2_DEVICE_VERIFICATION_COMPLETE_HEADER = "oauth2DeviceVerificationCompleteHeader";
    public static final String OAUTH2_DEVICE_VERIFICATION_FAILED = "oauth2DeviceVerificationFailedMessage";
    public static final String OAUTH2_DEVICE_VERIFICATION_FAILED_HEADER = "oauth2DeviceVerificationFailedHeader";
    public static final String OAUTH2_DEVICE_CONSENT_DENIED = "oauth2DeviceConsentDeniedMessage";

    public static final String CONFIRM_ORGANIZATION_MEMBERSHIP = "organization.confirm-membership";
    public static final String CONFIRM_ORGANIZATION_MEMBERSHIP_TITLE = "organization.confirm-membership.title";
    public static final String REGISTER_ORGANIZATION_MEMBER = "organization.member.register.title";
}
