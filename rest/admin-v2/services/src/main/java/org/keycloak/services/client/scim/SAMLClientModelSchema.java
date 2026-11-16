/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.client.scim;

import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.scim.resource.schema.attribute.Attribute;

/**
 * Schema singleton for SAML clients and provides attribute-filtered
 * population of {@link SAMLClientRepresentation}.
 */
public final class SAMLClientModelSchema extends BaseClientModelSchema<SAMLClientRepresentation> {

    private static final String SAML_NAME_ID_FORMAT               = "saml_name_id_format";
    private static final String SAML_FORCE_NAME_ID_FORMAT         = "saml_force_name_id_format";
    private static final String SAML_AUTHN_STATEMENT              = "saml.authnstatement";
    private static final String SAML_SERVER_SIGNATURE             = "saml.server.signature";
    private static final String SAML_ASSERTION_SIGNATURE          = "saml.assertion.signature";
    private static final String SAML_CLIENT_SIGNATURE             = "saml.client.signature";
    private static final String SAML_FORCE_POST_BINDING           = "saml.force.post.binding";
    private static final String SAML_SIGNATURE_ALGORITHM          = "saml.signature.algorithm";
    private static final String SAML_SIGNATURE_CANONICALIZATION   = "saml_signature_canonicalization_method";
    private static final String SAML_SIGNING_CERTIFICATE          = "saml.signing.certificate";
    private static final String SAML_ALLOW_ECP_FLOW               = "saml.allow.ecp.flow";

    // attribute names, which match the representation field names
    private static final String ATTR_NAME_ID_FORMAT = "nameIdFormat";
    private static final String ATTR_FORCE_NAME_ID_FORMAT = "forceNameIdFormat";
    private static final String ATTR_INCLUDE_AUTHN_STATEMENT = "includeAuthnStatement";
    private static final String ATTR_SIGN_DOCUMENTS = "signDocuments";
    private static final String ATTR_SIGN_ASSERTIONS = "signAssertions";
    private static final String ATTR_CLIENT_SIGNATURE_REQUIRED = "clientSignatureRequired";
    private static final String ATTR_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    private static final String ATTR_SIGNATURE_CANONICALIZATION_METHOD = "signatureCanonicalizationMethod";
    private static final String ATTR_SIGNING_CERTIFICATE = "signingCertificate";
    private static final String ATTR_FORCE_POST_BINDING = "forcePostBinding";
    private static final String ATTR_FRONT_CHANNEL_LOGOUT = "frontChannelLogout";
    private static final String ATTR_ALLOW_ECP_FLOW = "allowEcpFlow";

    public static final SAMLClientModelSchema INSTANCE = new SAMLClientModelSchema();

    private SAMLClientModelSchema() {
    }

    @Override
    protected void addProtocolAttributes(Map<String, Attribute<ClientModel, SAMLClientRepresentation>> map) {
        map.put(ATTR_NAME_ID_FORMAT,                    protocolStringAttr(ATTR_NAME_ID_FORMAT,                    SAML_NAME_ID_FORMAT,              (rep, v) -> rep.setNameIdFormat(SAMLClientRepresentation.NameIdFormat.fromJson(v)), (model, value) -> setAttribute(model, SAML_NAME_ID_FORMAT, value != null ? ((SAMLClientRepresentation.NameIdFormat) value).toJson() : null)));
        map.put(ATTR_FORCE_NAME_ID_FORMAT,              protocolBoolAttr  (ATTR_FORCE_NAME_ID_FORMAT,              SAML_FORCE_NAME_ID_FORMAT,        SAMLClientRepresentation::setForceNameIdFormat, (model, value) -> setBooleanAttribute(model, SAML_FORCE_NAME_ID_FORMAT, value)));
        map.put(ATTR_INCLUDE_AUTHN_STATEMENT,           protocolBoolAttr  (ATTR_INCLUDE_AUTHN_STATEMENT,           SAML_AUTHN_STATEMENT,             SAMLClientRepresentation::setIncludeAuthnStatement, (model, value) -> setBooleanAttribute(model, SAML_AUTHN_STATEMENT, value)));
        map.put(ATTR_SIGN_DOCUMENTS,                    protocolBoolAttr  (ATTR_SIGN_DOCUMENTS,                    SAML_SERVER_SIGNATURE,            SAMLClientRepresentation::setSignDocuments, (model, value) -> setBooleanAttribute(model, SAML_SERVER_SIGNATURE, value)));
        map.put(ATTR_SIGN_ASSERTIONS,                   protocolBoolAttr  (ATTR_SIGN_ASSERTIONS,                   SAML_ASSERTION_SIGNATURE,         SAMLClientRepresentation::setSignAssertions, (model, value) -> setBooleanAttribute(model, SAML_ASSERTION_SIGNATURE, value)));
        map.put(ATTR_CLIENT_SIGNATURE_REQUIRED,         protocolBoolAttr  (ATTR_CLIENT_SIGNATURE_REQUIRED,         SAML_CLIENT_SIGNATURE,            SAMLClientRepresentation::setClientSignatureRequired, (model, value) -> setBooleanAttribute(model, SAML_CLIENT_SIGNATURE, value)));
        map.put(ATTR_SIGNATURE_ALGORITHM,               protocolStringAttr(ATTR_SIGNATURE_ALGORITHM,               SAML_SIGNATURE_ALGORITHM,         (rep, v) -> rep.setSignatureAlgorithm(SAMLClientRepresentation.SignatureAlgorithm.fromJson(v)), (model, value) -> setAttribute(model, SAML_SIGNATURE_ALGORITHM, value != null ? ((SAMLClientRepresentation.SignatureAlgorithm) value).name() : null)));
        map.put(ATTR_SIGNATURE_CANONICALIZATION_METHOD, protocolStringAttr(ATTR_SIGNATURE_CANONICALIZATION_METHOD, SAML_SIGNATURE_CANONICALIZATION, SAMLClientRepresentation::setSignatureCanonicalizationMethod, (model, value) -> setAttribute(model, SAML_SIGNATURE_CANONICALIZATION, (String) value)));
        map.put(ATTR_SIGNING_CERTIFICATE,               protocolStringAttr(ATTR_SIGNING_CERTIFICATE,               SAML_SIGNING_CERTIFICATE,         SAMLClientRepresentation::setSigningCertificate, (model, value) -> setAttribute(model, SAML_SIGNING_CERTIFICATE, (String) value)));
        map.put(ATTR_FORCE_POST_BINDING,                protocolBoolAttr  (ATTR_FORCE_POST_BINDING,                SAML_FORCE_POST_BINDING,          SAMLClientRepresentation::setForcePostBinding, (model, value) -> setBooleanAttribute(model, SAML_FORCE_POST_BINDING, value)));
        map.put(ATTR_FRONT_CHANNEL_LOGOUT,              protocolBoolAttr  (ATTR_FRONT_CHANNEL_LOGOUT,              "frontchannelLogout",             SAMLClientRepresentation::setFrontChannelLogout, (model, logout) -> model.setFrontchannelLogout(Boolean.TRUE.equals(logout))));
        map.put(ATTR_ALLOW_ECP_FLOW,                    protocolBoolAttr  (ATTR_ALLOW_ECP_FLOW,                    SAML_ALLOW_ECP_FLOW,              SAMLClientRepresentation::setAllowEcpFlow, (model, value) -> setBooleanAttribute(model, SAML_ALLOW_ECP_FLOW, value)));
    }

    @Override
    protected void addProtocolDefaults(Map<String, Object> defaults) {
        // nameIdFormat is not included because its default is conditional, see applyDefaults
        defaults.put(ATTR_FORCE_NAME_ID_FORMAT, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_FORCE_NAME_ID_FORMAT));
        defaults.put(ATTR_INCLUDE_AUTHN_STATEMENT, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_INCLUDE_AUTHN_STATEMENT));
        defaults.put(ATTR_SIGN_DOCUMENTS, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_SIGN_DOCUMENTS));
        defaults.put(ATTR_SIGN_ASSERTIONS, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_SIGN_ASSERTIONS));
        defaults.put(ATTR_CLIENT_SIGNATURE_REQUIRED, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_CLIENT_SIGNATURE_REQUIRED));
        defaults.put(ATTR_FORCE_POST_BINDING, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_FORCE_POST_BINDING));
        defaults.put(ATTR_FRONT_CHANNEL_LOGOUT, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_FRONT_CHANNEL_LOGOUT));
        defaults.put(ATTR_ALLOW_ECP_FLOW, Boolean.valueOf(SAMLClientRepresentation.DEFAULT_ALLOW_ECP_FLOW));
        defaults.put(ATTR_SIGNATURE_ALGORITHM, SAMLClientRepresentation.DEFAULT_SIGNATURE_ALGORITHM);
        defaults.put(ATTR_SIGNATURE_CANONICALIZATION_METHOD, SAMLClientRepresentation.DEFAULT_SIGNATURE_CANONICALIZATION_METHOD);
    }

    @Override
    protected Object getAttributeValue(ClientModel model, String name) {
        return switch (name) {
            case ATTR_NAME_ID_FORMAT                    -> model.getAttribute(SAML_NAME_ID_FORMAT);
            case ATTR_FORCE_NAME_ID_FORMAT              -> getBooleanAttribute(model, SAML_FORCE_NAME_ID_FORMAT);
            // for the attributes that default to true on write, an absent attribute is effectively false (see SamlClient),
            // so report that to ensure that a patch that omits the field does not change the stored behavior
            case ATTR_INCLUDE_AUTHN_STATEMENT           -> getEffectiveBooleanAttribute(model, SAML_AUTHN_STATEMENT);
            case ATTR_SIGN_DOCUMENTS                    -> getEffectiveBooleanAttribute(model, SAML_SERVER_SIGNATURE);
            case ATTR_SIGN_ASSERTIONS                   -> getBooleanAttribute(model, SAML_ASSERTION_SIGNATURE);
            case ATTR_CLIENT_SIGNATURE_REQUIRED         -> getEffectiveBooleanAttribute(model, SAML_CLIENT_SIGNATURE);
            case ATTR_SIGNATURE_ALGORITHM               -> model.getAttribute(SAML_SIGNATURE_ALGORITHM);
            case ATTR_SIGNATURE_CANONICALIZATION_METHOD -> model.getAttribute(SAML_SIGNATURE_CANONICALIZATION);
            case ATTR_SIGNING_CERTIFICATE               -> model.getAttribute(SAML_SIGNING_CERTIFICATE);
            case ATTR_FORCE_POST_BINDING                -> getEffectiveBooleanAttribute(model, SAML_FORCE_POST_BINDING);
            case ATTR_FRONT_CHANNEL_LOGOUT              -> model.isFrontchannelLogout();
            case ATTR_ALLOW_ECP_FLOW                    -> getBooleanAttribute(model, SAML_ALLOW_ECP_FLOW);
            default                                     -> super.getAttributeValue(model, name);
        };
    }

    @Override
    public Object getRepresentationValue(SAMLClientRepresentation rep, String name) {
        return switch (name) {
            case ATTR_NAME_ID_FORMAT                     -> rep.getNameIdFormat();
            case ATTR_FORCE_NAME_ID_FORMAT               -> rep.getForceNameIdFormat();
            case ATTR_INCLUDE_AUTHN_STATEMENT            -> rep.getIncludeAuthnStatement();
            case ATTR_SIGN_DOCUMENTS                     -> rep.getSignDocuments();
            case ATTR_SIGN_ASSERTIONS                    -> rep.getSignAssertions();
            case ATTR_CLIENT_SIGNATURE_REQUIRED          -> rep.getClientSignatureRequired();
            case ATTR_SIGNATURE_ALGORITHM                -> rep.getSignatureAlgorithm();
            case ATTR_SIGNATURE_CANONICALIZATION_METHOD  -> rep.getSignatureCanonicalizationMethod();
            case ATTR_SIGNING_CERTIFICATE                -> rep.getSigningCertificate();
            case ATTR_FORCE_POST_BINDING                 -> rep.getForcePostBinding();
            case ATTR_FRONT_CHANNEL_LOGOUT               -> rep.getFrontChannelLogout();
            case ATTR_ALLOW_ECP_FLOW                     -> rep.getAllowEcpFlow();
            default                                      -> super.getRepresentationValue(rep, name);
        };
    }

    @Override
    public void applyDefaults(SAMLClientRepresentation rep, ClientModel existing, boolean patch) {
        super.applyDefaults(rep, existing, patch);
        // nameIdFormat is not an attribute default because it is conditional: a legacy client without a stored name id
        // format negotiates the format with the request (see SamlProtocol), so on a patch the default is only applied
        // when there is a stored format to reset. The merged representation cannot distinguish an omitted field from
        // an explicit null for such a client.
        if (rep.getNameIdFormat() == null && (!patch || (existing != null && existing.getAttribute(SAML_NAME_ID_FORMAT) != null))) {
            rep.setNameIdFormat(SAMLClientRepresentation.NameIdFormat.USERNAME);
        }
    }

    private Boolean getBooleanAttribute(ClientModel model, String key) {
        String value = model.getAttribute(key);
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    private Boolean getEffectiveBooleanAttribute(ClientModel model, String key) {
        return Boolean.parseBoolean(model.getAttribute(key));
    }

    private void setAttribute(ClientModel model, String key, String value) {
        if (value != null) {
            model.setAttribute(key, value);
        } else {
            model.removeAttribute(key);
        }
    }

    private void setBooleanAttribute(ClientModel model, String key, Boolean value) {
        if (value != null) {
            model.setAttribute(key, value.toString());
        } else {
            model.removeAttribute(key);
        }
    }

    @Override
    public SAMLClientRepresentation createRepresentation() {
        return new SAMLClientRepresentation();
    }
}
