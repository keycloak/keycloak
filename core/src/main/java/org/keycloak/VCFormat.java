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

package org.keycloak;


/**
 * Supported credential formats
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public interface VCFormat {
    /**
     * LD-Credentials {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-vc-secured-using-data-integ}
     */
    String LDP_VC = "ldp_vc";

    /**
     * JWT-Credentials {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-w3c-verifiable-credentials}
     */
    String JWT_VC = "jwt_vc_json";

    /**
     * SD-JWT-Credentials {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-ietf-sd-jwt-vc}
     */
    String SD_JWT_VC = "dc+sd-jwt";

    String[] SUPPORTED_FORMATS = new String[]{JWT_VC, SD_JWT_VC};

    static String getFromScope(String scope) {
        String format = SD_JWT_VC; // default format
        if (scope.toLowerCase().endsWith("_jwt")) format = JWT_VC;
        return format;
    }

    static String getScopeSuffix(String value) {
        String suffix = "";
        if (JWT_VC.equals(value)) suffix = "_jwt";
        else if (SD_JWT_VC.equals(value)) suffix = "_sd";
        return suffix;
    }
}
