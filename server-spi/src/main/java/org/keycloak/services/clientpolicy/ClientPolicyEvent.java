/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy;

/**
 * Events on which client policies mechanism detects and do its operation
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public enum ClientPolicyEvent {

    /** A new client is being registered via the admin REST API or dynamic client registration. */
    REGISTER,
    /** A client was successfully registered. Fires after {@link #REGISTER} completes. */
    REGISTERED,
    /** An existing client's settings are being updated. */
    UPDATE,
    /** A client was successfully updated. Fires after {@link #UPDATE} completes. */
    UPDATED,
    /** A client representation is being read. */
    VIEW,
    /** A client is being deleted. */
    UNREGISTER,
    /** An authorization request is received before standard processing. */
    PRE_AUTHORIZATION_REQUEST,
    /** An OIDC authorization request is being processed. */
    AUTHORIZATION_REQUEST,
    /** Tokens are being returned from an implicit or hybrid flow. */
    IMPLICIT_HYBRID_TOKEN_RESPONSE,
    /** An authorization code is being exchanged for tokens. */
    TOKEN_REQUEST,
    /** Tokens have been issued in response to a token request. */
    TOKEN_RESPONSE,
    /** A {@code client_credentials} grant request is being processed. */
    SERVICE_ACCOUNT_TOKEN_REQUEST,
    /** Tokens have been issued in response to a service-account token request. */
    SERVICE_ACCOUNT_TOKEN_RESPONSE,
    /** A refresh-token grant request is being processed. */
    TOKEN_REFRESH,
    /** Tokens have been issued in response to a refresh-token request. */
    TOKEN_REFRESH_RESPONSE,
    /** A token revocation request is being processed. */
    TOKEN_REVOKE,
    /** A token revocation request has been processed. */
    TOKEN_REVOKE_RESPONSE,
    /** A token introspection request is being processed. */
    TOKEN_INTROSPECT,
    /** A UserInfo endpoint request is being processed. */
    USERINFO_REQUEST,
    /** An OIDC logout request is being processed. */
    LOGOUT_REQUEST,
    /** A CIBA backchannel authentication request is being processed. */
    BACKCHANNEL_AUTHENTICATION_REQUEST,
    /** A CIBA backchannel token request is being processed. */
    BACKCHANNEL_TOKEN_REQUEST,
    /** Tokens have been issued in response to a CIBA backchannel token request. */
    BACKCHANNEL_TOKEN_RESPONSE,
    /** A Pushed Authorization Request (PAR) is being processed. */
    PUSHED_AUTHORIZATION_REQUEST,
    /** A device authorization request is being processed. */
    DEVICE_AUTHORIZATION_REQUEST,
    /** A device token request is being processed. */
    DEVICE_TOKEN_REQUEST,
    /** Tokens have been issued in response to a device token request. */
    DEVICE_TOKEN_RESPONSE,
    /** A token exchange request is being processed. */
    TOKEN_EXCHANGE_REQUEST,
    /** A Resource Owner Password Credentials grant request is being processed. */
    RESOURCE_OWNER_PASSWORD_CREDENTIALS_REQUEST,
    /** Tokens have been issued in response to a ROPC grant. */
    RESOURCE_OWNER_PASSWORD_CREDENTIALS_RESPONSE,
    /** A JWT authorization grant is being processed. */
    JWT_AUTHORIZATION_GRANT,
    /** An identity-brokering API call is being processed. */
    IDENTITY_BROKERING_API,

    /** A SAML authentication request is being processed. */
    SAML_AUTHN_REQUEST,
    /** A SAML logout request is being processed. */
    SAML_LOGOUT_REQUEST,

    /** A protocol mapper is being added to a client or client scope via the admin REST API. */
    REGISTER_PROTOCOL_MAPPER,
    /** An existing protocol mapper on a client or client scope is being updated via the admin REST API. */
    UPDATE_PROTOCOL_MAPPER,
    /** A protocol mapper is being removed from a client or client scope via the admin REST API. */
    UNREGISTER_PROTOCOL_MAPPER,

    /** One or more scope mappings are being added to a client or client scope via the admin REST API. */
    REGISTER_SCOPE_MAPPING,
    /** One or more scope mappings are being removed from a client or client scope via the admin REST API. */
    UNREGISTER_SCOPE_MAPPING,

    /** One or more role mappings are being added to a user or group via the admin REST API. */
    REGISTER_ROLE_MAPPING,
    /** One or more role mappings are being removed from a user or group via the admin REST API. */
    UNREGISTER_ROLE_MAPPING,

    /** A client certificate or keypair is being uploaded or generated via the admin REST API. */
    UPDATE_CLIENT_CERTIFICATE,
}
