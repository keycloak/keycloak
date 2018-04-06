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

package org.keycloak.authentication;

/**
 * Set of error codes that can be thrown by an Authenticator, FormAuthenticator, or FormAction
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public enum AuthenticationFlowError {
    EXPIRED_CODE,
    INVALID_CLIENT_SESSION,
    INVALID_USER,
    INVALID_CREDENTIALS,
    CREDENTIAL_SETUP_REQUIRED,
    USER_DISABLED,
    USER_CONFLICT,
    USER_TEMPORARILY_DISABLED,
    INTERNAL_ERROR,
    UNKNOWN_USER,
    FORK_FLOW,
    UNKNOWN_CLIENT,
    CLIENT_NOT_FOUND,
    CLIENT_DISABLED,
    CLIENT_CREDENTIALS_SETUP_REQUIRED,
    INVALID_CLIENT_CREDENTIALS,

    IDENTITY_PROVIDER_NOT_FOUND,
    IDENTITY_PROVIDER_DISABLED,
    IDENTITY_PROVIDER_ERROR,
    DISPLAY_NOT_SUPPORTED
}
