/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tracing;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Pre-allocated attribute keys for Tracing
 */
public interface TracingAttributes {
    String KC_PREFIX = "kc.";
    String KC_TOKEN_PREFIX = KC_PREFIX + "token.";

    AttributeKey<String> REALM_ID = AttributeKey.stringKey(KC_PREFIX + "realmId");
    AttributeKey<String> REALM_NAME = AttributeKey.stringKey(KC_PREFIX + "realmName");
    AttributeKey<String> CLIENT_ID = AttributeKey.stringKey(KC_PREFIX + "clientId");
    AttributeKey<String> USER_ID = AttributeKey.stringKey(KC_PREFIX + "userId");
    AttributeKey<String> AUTH_SESSION_ID = AttributeKey.stringKey(KC_PREFIX + "authenticationSessionId");
    AttributeKey<String> AUTH_TAB_ID = AttributeKey.stringKey(KC_PREFIX + "authenticationTabId");
    AttributeKey<String> SESSION_ID = AttributeKey.stringKey(KC_PREFIX + "sessionId");
    AttributeKey<String> EVENT_ID = AttributeKey.stringKey(KC_PREFIX + "eventId");
    AttributeKey<String> EVENT_ERROR = AttributeKey.stringKey(KC_PREFIX + "eventError");

    // Token
    AttributeKey<String> TOKEN_ISSUER = AttributeKey.stringKey(KC_TOKEN_PREFIX + "issuer");
    AttributeKey<String> TOKEN_SID = AttributeKey.stringKey(KC_TOKEN_PREFIX + "sid");
    AttributeKey<String> TOKEN_ID = AttributeKey.stringKey(KC_TOKEN_PREFIX + "id");

}
