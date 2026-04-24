/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vp;

public final class OID4VPConstants {

    public static final String MEDIA_TYPE_AUTHORIZATION_REQUEST_JWT = "application/oauth-authz-req+jwt";
    public static final String REQUEST_OBJECT_TYPE = "oauth-authz-req+jwt";

    public static final String AUD_SELF_ISSUED_V2 = "https://self-issued.me/v2";
    public static final String RESPONSE_TYPE_VP_TOKEN = "vp_token";
    public static final String RESPONSE_MODE_DIRECT_POST = "direct_post";
    public static final String DEFAULT_WALLET_SCHEME = "openid4vp://";

    public static final String VP_TOKEN = "vp_token";
    public static final String ID_TOKEN = "id_token";
    public static final String STATE = "state";
    public static final String RESPONSE_CODE = "response_code";
    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String REDIRECT_URI = "redirect_uri";

    private OID4VPConstants() {
    }
}
