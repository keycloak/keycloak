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

import java.util.List;

import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

/**
 * A provider for OID4VCI Credential Offers.
 *
 * In principle, it is the Issuer who creates a Credential Offer and then passes it to the Wallet. This can be done
 * by {@code credential_offer_uri}, by qr-code (for cross device flows), by a Wallet provided {@code credential_offer_endpoint}
 * or by some other means (e.g. email, messaging, etc.).
 *
 * The spec does not detail how to request a Credential Offer from an Issuer. In Keycloak, we provide an {@code /create-credential-offer}
 * endpoint that requires various access privileges depending on the offer that is to be created. Other means to create a
 * Credential Offer may exist in the Admin Console.
 *
 * Depending on Credential Offer Policies for a given {@code credential_configuration_id} and Issuing User, these required
 * privileges may be permissible enough to allow for a "self-issued" Credential Offer.
 *
 * With the Credential Offer the Wallet receives a grant for set of {@code credential_configuration_ids}.
 *
 * Credential Offers come in two variants
 * <ul>
 *     <li>{@code authorization_code} grant</li>
 *     <li>{@code pre-authorized_code} grant</li>
 * </ul>
 *
 * <h4>Authorization Code Grant</h4>
 *
 * With the {@code authorization_code} grant, the Wallet can send an Authorization Request that references one of the
 * {@code credential_configuration_ids} in {@code authorization_details}. Part of the {@code authorization_code} grant
 * is also some opaque {@code issuer_state} that the Wallet must include in the Authorization Request. This allows the
 * Issuer to correlate the Authorization Request with a previously made Credential Offer.
 *
 * The Wallet may also send an Authorization Request with a {@code scope} parameter and no {@code authorization_details}
 * nor {@code issuer_state}. This by-passes the Credential Offer process completely - the Authorization Server may
 * still return an Authorization Code, which can then be used in an AccessToken request and ultimately grants access
 * to Credentials associated with that {@code scope}.
 *
 * Whether the Authorization Server grants access or the Credential Endpoint actually returns the wanted Credential again
 * depends on the effective Credential Policies. These may require an existing Credential Offer for a given and hence prevent
 * such a Credential Request on scope alone.
 *
 * <h4>Pre-Authorized Code Grant</h4>
 *
 * As the name suggests, this is a "pre-authorized" grant that the Wallet can use directly in an AccessToken Request. No
 * further authorization is required on part of the Wallet. It works for cases where the Wallet cannot be expected to use
 * an {@code authorization_code} grant. Since this is bearer grant that gives direct access to the Credentials referenced
 * by {@code credential_configuration_ids}, the party that creates such a Credential Offer must take upmost be care about
 * how to communicate that "pre-authorized" grant. As a second authorization factor, the Issuer can create a {@code tx_code}
 * together with the Credential Offer. The {@code tx_code} is to be communicated via an alternative channel
 * (i.e. not together with the pre-authorized code).
 *
 * https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public interface CredentialOfferProvider extends Provider {

    CredentialOfferState createCredentialOffer(
            UserSessionModel userSession,
            String grantType,
            List<String> credentialConfigurationIds,
            String targetClientId,
            String targetUserId,
            Integer expireAt
    );

    @Override
    default void close() {
    }
}
