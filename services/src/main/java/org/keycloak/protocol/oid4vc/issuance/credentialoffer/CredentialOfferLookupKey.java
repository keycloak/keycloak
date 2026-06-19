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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

/**
 * Embeds the single-use cache key ({@code credentialsOfferId}) into externally visible lookup values
 * (nonce, credential identifiers) so only one entry is stored in {@code singleUseObjects}.
 */
public final class CredentialOfferLookupKey {

    private static final char SEPARATOR = ':';

    private CredentialOfferLookupKey() {
    }

    public static String embed(String publicPart, String offerId) {
        return publicPart + SEPARATOR + offerId;
    }

    public static String extractOfferId(String lookupValue) {
        if (lookupValue == null) {
            return null;
        }
        int separatorIndex = lookupValue.lastIndexOf(SEPARATOR);
        if (separatorIndex < 0 || separatorIndex == lookupValue.length() - 1) {
            return null;
        }
        return lookupValue.substring(separatorIndex + 1);
    }
}
