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
public interface Details {
    String PREF_PREVIOUS = "previous_";
    String PREF_UPDATED = "updated_";
    String FIELDS_TO_UPDATE = "fields_to_update";
    
    String CUSTOM_REQUIRED_ACTION="custom_required_action";
    String CONTEXT = "context";
    String EMAIL = "email";
    String PREVIOUS_EMAIL = PREF_PREVIOUS + "email";
    String UPDATED_EMAIL = PREF_UPDATED + "email";
    String ACTION = "action";
    String CODE_ID = "code_id";
    String REDIRECT_URI = "redirect_uri";
    String RESPONSE_TYPE = "response_type";
    String RESPONSE_MODE = "response_mode";
    String GRANT_TYPE = "grant_type";
    String AUTH_TYPE = "auth_type";
    String AUTH_METHOD = "auth_method";
    String IDENTITY_PROVIDER = "identity_provider";
    String IDENTITY_PROVIDER_USERNAME = "identity_provider_identity";
    String IDENTITY_PROVIDER_BROKER_SESSION_ID = "identity_provider_broker_session_id";
    String REGISTER_METHOD = "register_method";
    String USERNAME = "username";
    String FIRST_NAME = "first_name";
    String LAST_NAME = "last_name";
    String PREVIOUS_FIRST_NAME = PREF_PREVIOUS + "first_name";
    String UPDATED_FIRST_NAME = PREF_UPDATED + "first_name";
    String PREVIOUS_LAST_NAME = PREF_PREVIOUS + "last_name";
    String UPDATED_LAST_NAME = PREF_UPDATED + "last_name";
    String REMEMBER_ME = "remember_me";
    String TOKEN_ID = "token_id";
    String TOKEN_TYPE = "token_type";
    String TOKEN_ISSUED_FOR = "token_issued_for";
    String ORG_ID = "org_id";
    String REFRESH_TOKEN_ID = "refresh_token_id";
    String REFRESH_TOKEN_TYPE = "refresh_token_type";
    String REFRESH_TOKEN_SUB = "refresh_token_sub";
    String CLIENT_ASSERTION_ID = "client_assertion_id";
    String CLIENT_ASSERTION_SUB = "client_assertion_sub";
    String CLIENT_ASSERTION_ISSUER = "client_assertion_issuer";
    String VALIDATE_ACCESS_TOKEN = "validate_access_token";
    String UPDATED_REFRESH_TOKEN_ID = "updated_refresh_token_id";
    String NODE_HOST = "node_host";
    String REASON = "reason";
    String GRANTED_CLIENT = "granted_client";
    String REVOKED_CLIENT = "revoked_client";
    String TOKEN_EXCHANGE_REVOKED_CLIENTS = "token_exchange_revoked_clients";
    String AUDIENCE = "audience";
    String PERMISSION = "permission";
    String SCOPE = "scope";
    String REQUESTED_ISSUER = "requested_issuer";
    String REQUESTED_SUBJECT = "requested_subject";
    String REQUESTED_TOKEN_TYPE = "requested_token_type";
    String SUBJECT_TOKEN_CLIENT_ID = "subject_token_client_id";
    String RESTART_AFTER_TIMEOUT = "restart_after_timeout";
    String REDIRECTED_TO_CLIENT = "redirected_to_client";
    String LOGIN_RETRY = "login_retry";

    String CONSENT = "consent";
    String CONSENT_VALUE_NO_CONSENT_REQUIRED = "no_consent_required"; // No consent is required by client
    String CONSENT_VALUE_CONSENT_GRANTED = "consent_granted";         // Consent granted by user
    String CONSENT_VALUE_PERSISTED_CONSENT = "persistent_consent";    // Persistent consent used (was already granted by user before)
    String IMPERSONATOR_REALM = "impersonator_realm";
    String IMPERSONATOR = "impersonator";

    String CLIENT_AUTH_METHOD = "client_auth_method";

    String SIGNATURE_REQUIRED = "signature_required";
    String SIGNATURE_ALGORITHM = "signature_algorithm";

    String CLIENT_REGISTRATION_POLICY = "client_registration_policy";

    String EXISTING_USER = "previous_user";

    String X509_CERTIFICATE_SERIAL_NUMBER = "x509_cert_serial_number";
    String X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME = "x509_cert_subject_distinguished_name";
    String X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME = "x509_cert_issuer_distinguished_name";

    String CREDENTIAL_TYPE = "credential_type";
    String SELECTED_CREDENTIAL_ID = "selected_credential_id";
    String CREDENTIAL_ID = "credential_id";
    String AUTHENTICATION_ERROR_DETAIL = "authentication_error_detail";
    String CREDENTIAL_USER_LABEL = "credential_user_label";

    String NOT_BEFORE = "not_before";
    String NUM_FAILURES = "num_failures";

    String LOGOUT_TRIGGERED_BY_ACTION_TOKEN = "logout_triggered_by_action_token";
    String LOGOUT_TRIGGERED_BY_REQUIRED_ACTION = "logout_triggered_by_required_action";
    String ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";
    String AGE_OF_REFRESH_TOKEN = "age_of_refresh_token";

    String CLIENT_POLICY_ERROR = "client_policy_error";
    String CLIENT_POLICY_ERROR_DETAIL = "client_policy_error_detail";

    String EXPIRED_DETAIL = "expired";
}
