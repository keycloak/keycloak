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

package org.keycloak.models;

import org.keycloak.OAuth2Constants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Constants {
    String ADMIN_CONSOLE_CLIENT_ID = "security-admin-console";
    String ADMIN_CLI_CLIENT_ID = "admin-cli";

    String ACCOUNT_MANAGEMENT_CLIENT_ID = "account";
    String BROKER_SERVICE_CLIENT_ID = "broker";
    String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    String INSTALLED_APP_URN = "urn:ietf:wg:oauth:2.0:oob";
    String INSTALLED_APP_URL = "http://localhost";
    String READ_TOKEN_ROLE = "read-token";
    String[] BROKER_SERVICE_ROLES = {READ_TOKEN_ROLE};
    String OFFLINE_ACCESS_ROLE = OAuth2Constants.OFFLINE_ACCESS;

    String AUTHZ_UMA_PROTECTION = "uma_protection";
    String AUTHZ_UMA_AUTHORIZATION = "uma_authorization";
    String[] AUTHZ_DEFAULT_AUTHORIZATION_ROLES = {AUTHZ_UMA_AUTHORIZATION};

    // 15 minutes
    int DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT = 900;
    // 30 days
    int DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT = 2592000;

    String VERIFY_EMAIL_KEY = "VERIFY_EMAIL_KEY";
    String KEY = "key";

    // Prefix for user attributes used in various "context"data maps
    public static final String USER_ATTRIBUTES_PREFIX = "user.attributes.";
}
