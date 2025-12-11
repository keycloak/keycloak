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
 * Enum of supported credential formats
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public enum VCFormat {
    /**
     * LD-Credentials {@see https://www.w3.org/TR/vc-data-model/}
     */
    LDP_VC("ldp_vc"),

    /**
     * JWT-Credentials {@see https://identity.foundation/jwt-vc-presentation-profile/}
     */
    JWT_VC("jwt_vc"),

    /**
     * SD-JWT-Credentials {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
     */
    SD_JWT_VC("dc+sd-jwt");

    private final String value;

    VCFormat(String value) {
        this.value = value;
    }

    public static VCFormat fromValue(String value) {
        for (VCFormat f : values()) {
            if (f.value.equals(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown VC format: " + value);
    }

    public static VCFormat fromScopeName(String name) {
        if (name.endsWith("_jwt")) return JWT_VC;
        if (name.endsWith("_ld")) return LDP_VC;
        return SD_JWT_VC;
    }

    public String getValue() {
        return value;
    }

    public String getSuffix() {
        switch (this) {
            case JWT_VC: return "_jwt";
            case LDP_VC: return "_ld";
            case SD_JWT_VC: return "_sd";
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
