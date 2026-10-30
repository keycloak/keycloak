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
package org.keycloak.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * Catalog of URI fields stored in a client model or representation.
 * Values are returned as configured, without resolving references or applying validation rules.
 */
public enum ClientUriField {
    ROOT_URL("rootUrl", Source.ROOT,
            new ValidationMessages("rootUrl",
                "Root URL is not a valid URL", "clientRootURLInvalid",
                "Root URL must not contain an URL fragment", "clientRootURLFragmentError",
                "Root URL uses an illegal scheme", "clientRootURLIllegalSchemeError")),

    ADMIN_URL("adminUrl", Source.ADMIN,
            new ValidationMessages("adminUrl",
                "Admin URL is not a valid URL", "adminURLInvalid",
                null, null,
                "Admin URL uses an illegal scheme", "adminURLIllegalSchemeError")),

    BASE_URL("baseUrl", Source.BASE,
            new ValidationMessages("baseUrl",
                "Base URL is not a valid URL", "clientBaseURLInvalid",
                null, null,
                "Base URL uses an illegal scheme", "clientBaseURLIllegalSchemeError")),

    REDIRECT_URIS("redirectUris", Source.REDIRECTS,
            new ValidationMessages("redirectUris",
                "A redirect URI is not a valid URI", "clientRedirectURIsInvalid",
                "A redirect URI must not contain an URL fragment", "clientRedirectURIsFragmentError",
                "A redirect URI uses an illegal scheme", "clientRedirectURIsIllegalSchemeError")),

    WEB_ORIGINS("webOrigins", Source.ORIGINS,
            new ValidationMessages("webOrigins",
                "A web origin is not a valid URI", "webOriginsInvalid",
                null, null,
                "A web origin uses an illegal scheme", "webOriginsIllegalSchemeError")),

    JWKS_URL("jwksUri", OIDCConfigAttributes.JWKS_URL,
            new ValidationMessages("jwksUrl",
                "JWKS URL is not a valid URL", "jwksURLInvalid",
                null, null,
                "JWKS URL uses an illegal scheme", "jwksURLIllegalSchemeError")),

    REQUEST_URIS("requestUris", OIDCConfigAttributes.REQUEST_URIS, Source.MULTIVALUED,
            new ValidationMessages("requestUris",
                "Valid request URIs has an invalid value", "requestUrisAreInvalid",
                null, null,
                "Valid request URIs has a value with illegal scheme", "requestUrisHasIllegalSchemeError")),

    BACKCHANNEL_LOGOUT_URL("backchannelLogoutUrl", OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
            new ValidationMessages("backchannelLogoutUrl",
                "Backchannel logout URL is not a valid URL", "backchannelLogoutUrlIsInvalid",
                null, null,
                "Backchannel logout URL uses an illegal scheme", "backchannelLogoutUrlIllegalSchemeError")),

    FRONTCHANNEL_LOGOUT_URL("frontChannelLogoutUrl", OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI,
            new ValidationMessages("frontchannelLogoutUrl",
                "Front-channel logout URL is not a valid URL", "frontchannelLogoutUrlInvalid",
                null, null,
                "Front-channel logout URL uses an illegal scheme", "frontchannelLogoutUrlIllegalSchemeError")),

    POST_LOGOUT_REDIRECT_URIS("postLogoutRedirectUris", OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, Source.MULTIVALUED,
            new ValidationMessages("postLogoutUris",
                "A post-logout redirect URI is not a valid URI", "clientPostLogoutRedirectURIsInvalid",
                "A post-logout redirect URI must not contain an URL fragment", "clientPostLogoutRedirectURIsFragmentError",
                "A post-logout redirect URI uses an illegal scheme", "clientPostLogoutRedirectURIsIllegalSchemeError")),

    CIBA_NOTIFICATION_ENDPOINT("cibaClientNotificationEndpoint", CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT,
            new ValidationMessages("cibaBackchannelClientNotificationEndpoint",
                "CIBA notification endpoint is not a valid URL", "invalidBackchannelClientNotificationEndpoint",
                null, null,
                "CIBA notification endpoint uses an illegal scheme", "invalidBackchannelClientNotificationEndpoint")),

    LOGO_URI(OIDCConfigAttributes.LOGO_URI,
            new ValidationMessages(ClientModel.LOGO_URI,
                "Logo URL is not a valid URL", "logoURLInvalid",
                null, null,
                "Logo URL uses an illegal scheme", "logoURLIllegalSchemeError")),

    POLICY_URI(OIDCConfigAttributes.POLICY_URI,
            new ValidationMessages(ClientModel.POLICY_URI,
                "Policy URL is not a valid URL", "policyURLInvalid",
                null, null,
                "Policy URL uses an illegal scheme", "policyURLIllegalSchemeError")),

    TOS_URI(OIDCConfigAttributes.TOS_URI,
            new ValidationMessages(ClientModel.TOS_URI,
                "Terms of service URL is not a valid URL", "tosURLInvalid",
                null, null,
                "Terms of service URL uses an illegal scheme", "tosURLIllegalSchemeError")),

    SECTOR_IDENTIFIER_URI(OIDCConfigAttributes.SECTOR_IDENTIFIER_URI,
            new ValidationMessages("sectorIdentifierUri",
                "Sector identifier URI is not a valid URL", "sectorIdentifierUriInvalid",
                null, null,
                "Sector identifier URI uses an illegal scheme", "sectorIdentifierUriIllegalSchemeError")),

    SAML_ASSERTION_CONSUMER_URL_POST_URI(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                "Assertion Consumer Service POST Binding URL is not a valid URL", "samlAssertionConsumerUrlPostURLInvalid",
                null, null,
                "Assertion Consumer Service POST Binding URL uses an illegal scheme", "samlAssertionConsumerUrlPostURLIllegalSchemeError")),

    SAML_ASSERTION_CONSUMER_URL_REDIRECT_URI(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE,
                "Assertion Consumer Service Redirect Binding URL is not a valid URL", "samlAssertionConsumerUrlRedirectURLInvalid",
                null, null,
                "Assertion Consumer Service Redirect Binding URL uses an illegal scheme", "samlAssertionConsumerUrlRedirectURLIllegalSchemeError")),

    SAML_ASSERTION_CONSUMER_URL_ARTIFACT_URI(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE,
                "Artifact Binding URL is not a valid URL", "samlAssertionConsumerUrlArtifactURLInvalid",
                null, null,
                "Artifact Binding URL uses an illegal scheme", "samlAssertionConsumerUrlArtifactURLIllegalSchemeError")),

    SAML_SINGLE_LOGOUT_SERVICE_URL_POST_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                "Logout Service POST Binding URL is not a valid URL", "samlLogoutServiceUrlPostURLInvalid",
                null, null,
                "Logout Service POST Binding URL uses an illegal scheme", "samlLogoutServiceUrlPostURLIllegalSchemeError")),

    SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE,
                "Logout Service ARTIFACT Binding URL is not a valid URL", "samlLogoutServiceUrlArtifactURLInvalid",
                null, null,
                "Logout Service ARTIFACT Binding URL uses an illegal scheme", "samlLogoutServiceUrlArtifactURLIllegalSchemeError")),

    SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE,
                "Logout Service Redirect Binding URL is not a valid URL", "samlLogoutServiceUrlRedirectURLInvalid",
                null, null,
                "Logout Service Redirect Binding URL uses an illegal scheme", "samlLogoutServiceUrlRedirectURLIllegalSchemeError")),

    SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE,
                "Logout Service SOAP Binding URL is not a valid URL", "samlLogoutServiceUrlSoapURLInvalid",
                null, null,
                "Logout Service SOAP Binding URL uses an illegal scheme", "samlAssertionConsumerUrlPostURLIllegalSchemeError")),

    SAML_ARTIFACT_RESOLUTION_SERVICE_URL_URI(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE,
            new ValidationMessages(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE,
                "Artifact Resolution Service is not a valid URL", "samlAssertionConsumerUrlPostURLInvalid",
                null, null,
                "Artifact Resolution Service uses an illegal scheme", "samlAssertionConsumerUrlPostURLIllegalSchemeError")),

    SAML_METADATA_DESCRIPTOR_URL(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
            new ValidationMessages(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                "Metadata descriptor URL is not a valid URL", SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                null, null,
                "Metadata descriptor URL uses an illegal scheme", SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL));

    private enum Source {
        ROOT, ADMIN, BASE, REDIRECTS, ORIGINS, ATTRIBUTE, MULTIVALUED
    }

    private final String name;
    private final String attribute;
    private final Source source;
    private final ValidationMessages messages;

    private static final ValidationMessages SAML_ADMIN_MESSAGES = new ValidationMessages("masterSamlProcessingUrl",
                "Master SAML Processing URL is not a valid URL", "adminUrlURLInvalid",
                null, null,
                "Master SAML Processing URL uses an illegal scheme", "adminUrlURLIllegalSchemeError");

    ClientUriField(String attribute, ValidationMessages messages) {
        this(attribute, attribute, messages);
    }

    ClientUriField(String name, String attribute, ValidationMessages messages) {
        this(name, attribute, Source.ATTRIBUTE, messages);
    }

    ClientUriField(String name, Source source, ValidationMessages messages) {
        this(name, null, source, messages);
    }

    ClientUriField(String name, String attribute, Source source, ValidationMessages messages) {
        this.name = name;
        this.attribute = attribute;
        this.source = source;
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public static List<String> getNames() {
        return Arrays.stream(values()).map(ClientUriField::getName).toList();
    }

    public static Optional<ClientUriField> fromName(String name) {
        return Arrays.stream(values()).filter(field -> field.name.equals(name)).findFirst();
    }

    public ValidationMessages getValidationMessages(String protocol) {
        return this == ADMIN_URL && SamlProtocol.LOGIN_PROTOCOL.equals(protocol) ? SAML_ADMIN_MESSAGES : messages;
    }

    public record ValidationMessages(String fieldId, String invalid, String invalidKey, String fragment,
                                     String fragmentKey, String scheme, String schemeKey) {
    }

    /**
     * Validate raw configured values with the caller's rules. Policy-specific resolution and
     * special values belong to the validator: for example, regex policies match literal values.
     */
    @FunctionalInterface
    public interface Validator<E extends Exception> {
        void validate(ClientUriField field, List<String> values) throws E;
    }

    public static <E extends Exception> void validate(ClientRepresentation client, Validator<E> validator) throws E {
        validate(client, List.of(values()), validator);
    }

    public static <E extends Exception> void validate(ClientRepresentation client, List<ClientUriField> fields,
                                                     Validator<E> validator) throws E {
        for (ClientUriField field : fields) {
            validator.validate(field, field.getValues(client));
        }
    }

    public static <E extends Exception> void validate(ClientModel client, Validator<E> validator) throws E {
        for (ClientUriField field : values()) {
            validator.validate(field, field.getValues(client));
        }
    }

    public List<String> getValues(ClientRepresentation client) {
        return switch (source) {
            case ROOT -> singleton(client.getRootUrl());
            case ADMIN -> singleton(client.getAdminUrl());
            case BASE -> singleton(client.getBaseUrl());
            case REDIRECTS -> nonEmpty(client.getRedirectUris());
            case ORIGINS -> nonEmpty(client.getWebOrigins());
            case ATTRIBUTE, MULTIVALUED -> attributeValues(client.getAttributes() == null ? null : client.getAttributes().get(attribute));
        };
    }

    public List<String> getValues(ClientModel client) {
        return switch (source) {
            case ROOT -> singleton(client.getRootUrl());
            case ADMIN -> singleton(client.getManagementUrl());
            case BASE -> singleton(client.getBaseUrl());
            case REDIRECTS -> nonEmpty(client.getRedirectUris());
            case ORIGINS -> nonEmpty(client.getWebOrigins());
            case ATTRIBUTE, MULTIVALUED -> attributeValues(client.getAttribute(attribute));
        };
    }

    private List<String> attributeValues(String value) {
        return source == Source.MULTIVALUED && value != null
                ? nonEmpty(Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(value))) : singleton(value);
    }

    private static List<String> singleton(String value) {
        return value == null || value.isEmpty() ? List.of() : List.of(value);
    }

    private static List<String> nonEmpty(Collection<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isEmpty()).toList();
    }
}
