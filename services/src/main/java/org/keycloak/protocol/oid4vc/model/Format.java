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

package org.keycloak.protocol.oid4vc.model;

import java.util.Collections;
import java.util.Set;

/**
 * Enum of supported credential formats
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class Format {

    /**
     * LD-Credentials {@see https://www.w3.org/TR/vc-data-model/}
     */
    public static final String LDP_VC = "ldp_vc";

    /**
     * JWT-Credentials {@see https://identity.foundation/jwt-vc-presentation-profile/}
     */
    public static final String JWT_VC = "jwt_vc";

    /**
     * SD-JWT-Credentials {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
     */
    public static final String SD_JWT_VC = "dc+sd-jwt";

    public static final Set<String> SUPPORTED_FORMATS = Collections.unmodifiableSet(Set.of(JWT_VC, LDP_VC, SD_JWT_VC));
}
