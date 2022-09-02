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
import org.keycloak.crypto.Algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class Constants {
    public static final String ADMIN_CONSOLE_CLIENT_ID = "security-admin-console";
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String ACCOUNT_MANAGEMENT_CLIENT_ID = "account";
    public static final String ACCOUNT_CONSOLE_CLIENT_ID = "account-console";
    public static final String BROKER_SERVICE_CLIENT_ID = "broker";
    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    public static final String AUTH_BASE_URL_PROP = "${authBaseUrl}";
    public static final String AUTH_ADMIN_URL_PROP = "${authAdminUrl}";

    public static final Collection<String> defaultClients = Arrays.asList(ACCOUNT_MANAGEMENT_CLIENT_ID, ADMIN_CLI_CLIENT_ID, BROKER_SERVICE_CLIENT_ID, REALM_MANAGEMENT_CLIENT_ID, ADMIN_CONSOLE_CLIENT_ID);

    public static final String INSTALLED_APP_URN = "urn:ietf:wg:oauth:2.0:oob";
    public static final String INSTALLED_APP_URL = "http://localhost";
    public static final String INSTALLED_APP_LOOPBACK = "http://127.0.0.1";

    public static final String READ_TOKEN_ROLE = "read-token";
    public static final String[] BROKER_SERVICE_ROLES = {READ_TOKEN_ROLE};
    public static final String OFFLINE_ACCESS_ROLE = OAuth2Constants.OFFLINE_ACCESS;
    public static final String DEFAULT_ROLES_ROLE_PREFIX = "default-roles";

    public static final String AUTHZ_UMA_PROTECTION = "uma_protection";
    public static final String AUTHZ_UMA_AUTHORIZATION = "uma_authorization";
    public static final String[] AUTHZ_DEFAULT_AUTHORIZATION_ROLES = {AUTHZ_UMA_AUTHORIZATION};

    // 15 minutes
    public static final int DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT = 900;
    // 30 days
    public static final int DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT = 2592000;
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    // 60 days
    public static final int DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN = 5184000;
    public static final String DEFAULT_SIGNATURE_ALGORITHM = Algorithm.RS256;

    public static final String DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS = Algorithm.ES256;
    public static final String DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME = "keycloak";
    // it stands for optional parameter not specified in WebAuthn
    public static final String DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED = "not specified";

    // Prefix used for the realm attributes and other places
    public static final String WEBAUTHN_PASSWORDLESS_PREFIX = "Passwordless";

    public static final String VERIFY_EMAIL_KEY = "VERIFY_EMAIL_KEY";
    public static final String EXECUTION = "execution";
    public static final String CLIENT_ID = "client_id";
    public static final String TAB_ID = "tab_id";
    public static final String KEY = "key";

    public static final String KC_ACTION = "kc_action";
    public static final String KC_ACTION_STATUS = "kc_action_status";
    public static final String KC_ACTION_EXECUTING = "kc_action_executing";
    public static final int KC_ACTION_MAX_AGE = 300;

    public static final String IS_AIA_REQUEST = "IS_AIA_REQUEST";
    public static final String AIA_SILENT_CANCEL = "silent_cancel";
    public static final String AUTHENTICATION_EXECUTION = "authenticationExecution";
    public static final String CREDENTIAL_ID = "credentialId";

    public static final String SKIP_LINK = "skipLink";
    public static final String TEMPLATE_ATTR_ACTION_URI = "actionUri";
    public static final String TEMPLATE_ATTR_REQUIRED_ACTIONS = "requiredActions";

    // Prefix for user attributes used in various "context"data maps
    public static final String USER_ATTRIBUTES_PREFIX = "user.attributes.";

    // Roles already granted by a mapper when updating brokered users.
    public static final String MAPPER_GRANTED_ROLES = "MAPPER_GRANTED_ROLES";

    // Groups already assigned by a mapper when updating brokered users.
    public static final String MAPPER_GRANTED_GROUPS = "MAPPER_GRANTED_GROUPS";

    // Indication to admin-rest-endpoint that realm keys should be re-generated
    public static final String GENERATE = "GENERATE";

    public static final int DEFAULT_MAX_RESULTS = 100;

    // Delimiter to be used in the configuration of authenticators (and some other components) in case that we need to save
    // multiple values into single string
    public static final String CFG_DELIMITER = "##";

    // Better performance to use this instead of String.split
    public static final Pattern CFG_DELIMITER_PATTERN = Pattern.compile("\\s*" + CFG_DELIMITER + "\\s*");

    public static final String OFFLINE_ACCESS_SCOPE_CONSENT_TEXT = "${offlineAccessScopeConsentText}";

    /**
     * If set as an attribute in the {@link KeycloakSession}, indicates that the storage should batch write operations.
     */
    public static final String STORAGE_BATCH_ENABLED = "org.keycloak.storage.batch_enabled";

    /**
     * If {@code #STORAGE_BATCH_ENABLED} is set, indicates the batch size.
     */
    public static final String STORAGE_BATCH_SIZE = "org.keycloak.storage.batch_size";

    // Client Polices Realm Attributes Keys
    public static final String CLIENT_PROFILES = "client-policies.profiles";
    public static final String CLIENT_POLICIES = "client-policies.policies";


    // Authentication session note, which contains loa of current authentication in progress
    public static final String LEVEL_OF_AUTHENTICATION = "level-of-authentication";

    // Authentication session (and user session) note, which contains map with authenticated levels and the times of their authentications,
    // so it is possible to check when particular level expires and needs to be re-authenticated
    public static final String LOA_MAP = "loa-map";

    public static final String REQUESTED_LEVEL_OF_AUTHENTICATION = "requested-level-of-authentication";
    public static final String FORCE_LEVEL_OF_AUTHENTICATION = "force-level-of-authentication";
    public static final String ACR_LOA_MAP = "acr.loa.map";
    public static final String DEFAULT_ACR_VALUES = "default.acr.values";
    public static final int MINIMUM_LOA = 0;
    public static final int NO_LOA = -1;
}
