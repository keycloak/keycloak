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

package org.keycloak;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface OAuth2Constants {

    String CODE = "code";

    String CLIENT_ID = "client_id";

    String CLIENT_SECRET = "client_secret";

    String ERROR = "error";

    String ERROR_DESCRIPTION = "error_description";

    String REDIRECT_URI = "redirect_uri";

    String DISPLAY = "display";

    String SCOPE = "scope";

    String STATE = "state";

    String GRANT_TYPE = "grant_type";

    String RESPONSE_TYPE = "response_type";

    String ACCESS_TOKEN = "access_token";

    String TOKEN_TYPE = "token_type";

    String EXPIRES_IN = "expires_in";

    String ID_TOKEN = "id_token";

    String REFRESH_TOKEN = "refresh_token";

    String LOGOUT_TOKEN = "logout_token";

    String AUTHORIZATION_CODE = "authorization_code";


    String IMPLICIT = "implicit";

    String USERNAME="username";

    String PASSWORD = "password";

    String CLIENT_CREDENTIALS = "client_credentials";

    // https://tools.ietf.org/html/draft-ietf-oauth-assertions-01#page-5
    String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    String CLIENT_ASSERTION = "client_assertion";

    // https://tools.ietf.org/html/draft-jones-oauth-jwt-bearer-03#section-2.2
    String CLIENT_ASSERTION_TYPE_JWT = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    // http://openid.net/specs/openid-connect-core-1_0.html#OfflineAccess
    String OFFLINE_ACCESS = "offline_access";

    // http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
    String SCOPE_OPENID = "openid";

    // http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims
    String SCOPE_PROFILE = "profile";
    String SCOPE_EMAIL = "email";
    String SCOPE_ADDRESS = "address";
    String SCOPE_PHONE = "phone";

    String UI_LOCALES_PARAM = "ui_locales";

    String PROMPT = "prompt";
    String ACR_VALUES = "acr_values";

    String MAX_AGE = "max_age";

    // OIDC Session Management
    String SESSION_STATE = "session_state";

    String JWT = "JWT";

    // https://tools.ietf.org/html/rfc7636#section-6.1
    String CODE_VERIFIER = "code_verifier";
    String CODE_CHALLENGE = "code_challenge";
    String CODE_CHALLENGE_METHOD = "code_challenge_method";

    // https://tools.ietf.org/html/rfc7636#section-6.2.2
    String PKCE_METHOD_PLAIN = "plain";
    String PKCE_METHOD_S256 = "S256";

    // https://tools.ietf.org/html/rfc8693#section-2.1
    String TOKEN_EXCHANGE_GRANT_TYPE="urn:ietf:params:oauth:grant-type:token-exchange";
    String AUDIENCE="audience";
    String RESOURCE="resource";
    String REQUESTED_SUBJECT="requested_subject";
    String SUBJECT_TOKEN="subject_token";
    String SUBJECT_TOKEN_TYPE="subject_token_type";
    String ACTOR_TOKEN="actor_token";
    String ACTOR_TOKEN_TYPE="actor_token_type";
    String REQUESTED_TOKEN_TYPE="requested_token_type";
    String ISSUED_TOKEN_TYPE="issued_token_type";
    String REQUESTED_ISSUER="requested_issuer";
    String SUBJECT_ISSUER="subject_issuer";
    String ACCESS_TOKEN_TYPE="urn:ietf:params:oauth:token-type:access_token";
    String REFRESH_TOKEN_TYPE="urn:ietf:params:oauth:token-type:refresh_token";
    String JWT_TOKEN_TYPE="urn:ietf:params:oauth:token-type:jwt";
    String ID_TOKEN_TYPE="urn:ietf:params:oauth:token-type:id_token";
    String SAML2_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:saml2";

    String UMA_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:uma-ticket";

    // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-3.4
    String DEVICE_CODE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";
    String DEVICE_CODE = "device_code";

    String CIBA_GRANT_TYPE = "urn:openid:params:grant-type:ciba";

    String DISPLAY_CONSOLE = "console";
    String INTERVAL = "interval";
    String USER_CODE = "user_code";

    // https://openid.net/specs/openid-financial-api-jarm-ID1.html
    String RESPONSE = "response";
}


