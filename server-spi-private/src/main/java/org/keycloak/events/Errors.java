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

package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Errors {

    String INVALID_REQUEST = "invalid_request";

    String REALM_DISABLED = "realm_disabled";

    String CLIENT_NOT_FOUND = "client_not_found";
    String CLIENT_DISABLED = "client_disabled";
    String INVALID_CLIENT_CREDENTIALS = "invalid_client_credentials";
    String INVALID_CLIENT = "invalid_client";
    String CONSENT_DENIED = "consent_denied";
    String RESOLVE_REQUIRED_ACTIONS = "resolve_required_actions";

    String USER_NOT_FOUND = "user_not_found";
    String USER_DISABLED = "user_disabled";
    String USER_TEMPORARILY_DISABLED = "user_temporarily_disabled";
    String INVALID_USER_CREDENTIALS = "invalid_user_credentials";
    String DIFFERENT_USER_AUTHENTICATED = "different_user_authenticated";
    String USER_DELETE_ERROR = "user_delete_error";

    String USERNAME_MISSING = "username_missing";
    String USERNAME_IN_USE = "username_in_use";
    String EMAIL_IN_USE = "email_in_use";

    String INVALID_REDIRECT_URI = "invalid_redirect_uri";
    String INVALID_CODE = "invalid_code";
    String INVALID_TOKEN = "invalid_token";
    String INVALID_TOKEN_TYPE = "invalid_token_type";
    String INVALID_SAML_RESPONSE = "invalid_saml_response";
    String INVALID_SAML_AUTHN_REQUEST = "invalid_authn_request";
    String INVALID_SAML_LOGOUT_REQUEST = "invalid_logout_request";
    String INVALID_SAML_LOGOUT_RESPONSE = "invalid_logout_response";
    String INVALID_SAML_ARTIFACT = "invalid_artifact";
    String INVALID_SAML_ARTIFACT_RESPONSE = "invalid_artifact_response";
    String SAML_TOKEN_NOT_FOUND = "saml_token_not_found";
    String INVALID_SIGNATURE = "invalid_signature";
    String INVALID_REGISTRATION = "invalid_registration";
    String INVALID_ISSUER = "invalid_issuer";
    String INVALID_FORM = "invalid_form";
    String INVALID_CONFIG = "invalid_config";
    String EXPIRED_CODE = "expired_code";
    String INVALID_INPUT = "invalid_input";
    String COOKIE_NOT_FOUND = "cookie_not_found";

    String REGISTRATION_DISABLED = "registration_disabled";
    String RESET_CREDENTIAL_DISABLED = "reset_credential_disabled";

    String REJECTED_BY_USER = "rejected_by_user";

    String NOT_ALLOWED = "not_allowed";

    String FEDERATED_IDENTITY_EXISTS = "federated_identity_account_exists";
    String SSL_REQUIRED = "ssl_required";

    String USER_SESSION_NOT_FOUND = "user_session_not_found";
    String SESSION_EXPIRED = "session_expired";

    String EMAIL_SEND_FAILED = "email_send_failed";
    String INVALID_EMAIL = "invalid_email";
    String IDENTITY_PROVIDER_LOGIN_FAILURE = "identity_provider_login_failure";
    String IDENTITY_PROVIDER_ERROR = "identity_provider_error";

    String PASSWORD_CONFIRM_ERROR = "password_confirm_error";
    String PASSWORD_MISSING = "password_missing";
    String PASSWORD_REJECTED = "password_rejected";

    // https://tools.ietf.org/html/rfc7636
    String CODE_VERIFIER_MISSING = "code_verifier_missing";
    String INVALID_CODE_VERIFIER = "invalid_code_verifier";
    String PKCE_VERIFICATION_FAILED = "pkce_verification_failed";
    String INVALID_CODE_CHALLENGE_METHOD = "invalid_code_challenge_method";


    String NOT_LOGGED_IN = "not_logged_in";
    String UNKNOWN_IDENTITY_PROVIDER = "unknown_identity_provider";
    String ILLEGAL_ORIGIN = "illegal_origin";
    String DISPLAY_UNSUPPORTED = "display_unsupported";
    String LOGOUT_FAILED = "logout_failed";
    String INVALID_DESTINATION = "invalid_destination";
    String MISSING_REQUIRED_DESTINATION = "missing_required_destination";
    String INVALID_SAML_DOCUMENT = "invalid_saml_document";
    String UNSUPPORTED_NAMEID_FORMAT = "unsupported_nameid_format";

    String INVALID_PERMISSION_TICKET = "invalid_permission_ticket";
    String ACCESS_DENIED = "access_denied";

    String INVALID_OAUTH2_DEVICE_CODE = "invalid_oauth2_device_code";
    String EXPIRED_OAUTH2_DEVICE_CODE = "expired_oauth2_device_code";
    String INVALID_OAUTH2_USER_CODE = "invalid_oauth2_user_code";
    String SLOW_DOWN = "slow_down";
    String GENERIC_AUTHENTICATION_ERROR= "generic_authentication_error";

}
