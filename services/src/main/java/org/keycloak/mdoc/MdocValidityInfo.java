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
package org.keycloak.mdoc;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

import com.authlete.mdoc.ValidityInfo;

/**
 * Validity information for the ISO mdoc Mobile Security Object.
 *
 * The MSO records when the issuer signed the credential, when it becomes valid, and when it expires.
 */
public class MdocValidityInfo {

    private final Instant signed;
    private final Instant validFrom;
    private final Instant validUntil;

    public MdocValidityInfo(Instant signed, Instant validFrom, Instant validUntil) {
        this.signed = Objects.requireNonNull(signed, "signed");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil");
    }

    /**
     * Creates validity information where signed and validFrom both equal the issuance time.
     *
     * @param issuanceInstant issuance time used for signed and validFrom
     * @param expirationInstant instant after which the mDoc is no longer valid
     * @return MSO validity information
     */
    public static MdocValidityInfo issuedAt(Instant issuanceInstant, Instant expirationInstant) {
        Instant issuance = Objects.requireNonNull(issuanceInstant, "issuanceInstant");
        Instant expiration = Objects.requireNonNull(expirationInstant, "expirationInstant");
        return new MdocValidityInfo(issuance, issuance, expiration);
    }

    public Instant getSigned() {
        return signed;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    ValidityInfo toAuthleteValidityInfo() {
        return new ValidityInfo(
                signed.atZone(ZoneOffset.UTC),
                validFrom.atZone(ZoneOffset.UTC),
                validUntil.atZone(ZoneOffset.UTC)
        );
    }
}
