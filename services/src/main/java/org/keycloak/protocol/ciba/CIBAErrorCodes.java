/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;

public interface CIBAErrorCodes {
    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#token_error_response
    // Token Error Response
    String AUTHORIZATION_PENDING = "authorization_pending";
    String SLOW_DOWN = "slow_down";
    String EXPIRED_TOKEN = "expired_token";
    String ACCESS_DENIED = OAuthErrorException.ACCESS_DENIED;

    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#push_error_payload
    // Push Error Payload
    String ERROR_DESCRIPTION = OAuth2Constants.ERROR_DESCRIPTION;
    String ERROR = OAuth2Constants.ERROR;
    String TRANSACTION_FAILED = "transaction_failed";

    // https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#auth_error_response
    // Authentication Error Response
    String ERROR_URI = "error_uri";
    // 400 Bad Request
    String INVALID_REQUEST = OAuthErrorException.INVALID_REQUEST;
    String INVALID_SCOPE = OAuthErrorException.INVALID_SCOPE;
    String EXPIRED_LOGIN_HINT_TOKEN = "expired_login_hint_token";
    String UNKNOWN_USER_ID = "unknown_user_id";
    String UNAUTHORIZED_CLIENT = OAuthErrorException.UNAUTHORIZED_CLIENT;
    String MISSING_USER_CODE = "missing_user_code";
    String INVALID_USER_CODE = "invalid_user_code";
    String INVALID_BINDING_MESSAGE = "invalid_binding_message";
    // 401 Unauthorized
    String INVALID_CLIENT = OAuthErrorException.INVALID_CLIENT;


}
