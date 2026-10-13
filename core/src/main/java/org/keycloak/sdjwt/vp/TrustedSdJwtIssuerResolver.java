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

package org.keycloak.sdjwt.vp;

import org.keycloak.common.VerificationException;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;

/**
 * Resolves the keys trusted to have issued a presented credential, e.g. against an issuer allowlist
 * or a trust list.
 */
@FunctionalInterface
public interface TrustedSdJwtIssuerResolver {
    TrustedSdJwtIssuer resolve(IssuerSignedJWT issuerSignedJwt) throws VerificationException;
}
