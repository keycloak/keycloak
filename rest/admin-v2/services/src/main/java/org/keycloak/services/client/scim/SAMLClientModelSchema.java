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

    public static final SAMLClientModelSchema INSTANCE = new SAMLClientModelSchema();

    private SAMLClientModelSchema() {
    }

    @Override
    protected void addProtocolAttributes(Map<String, Attribute<ClientModel, SAMLClientRepresentation>> map) {
        map.put("nameIdFormat",                 protocolStringAttr("nameIdFormat",                 SAML_NAME_ID_FORMAT,              (rep, v) -> rep.setNameIdFormat(SAMLClientRepresentation.NameIdFormat.fromJson(v)), (model, value) -> setAttribute(model, SAML_NAME_ID_FORMAT, value != null ? ((SAMLClientRepresentation.NameIdFormat) value).toJson() : null)));
        map.put("forceNameIdFormat",            protocolBoolAttr  ("forceNameIdFormat",            SAML_FORCE_NAME_ID_FORMAT,        SAMLClientRepresentation::setForceNameIdFormat, (model, value) -> setBooleanAttribute(model, SAML_FORCE_NAME_ID_FORMAT, value)));
        map.put("includeAuthnStatement",        protocolBoolAttr  ("includeAuthnStatement",        SAML_AUTHN_STATEMENT,             SAMLClientRepresentation::setIncludeAuthnStatement, (model, value) -> setBooleanAttribute(model, SAML_AUTHN_STATEMENT, value)));
        map.put("signDocuments",                protocolBoolAttr  ("signDocuments",                SAML_SERVER_SIGNATURE,            SAMLClientRepresentation::setSignDocuments, (model, value) -> setBooleanAttribute(model, SAML_SERVER_SIGNATURE, value)));
        map.put("signAssertions",               protocolBoolAttr  ("signAssertions",               SAML_ASSERTION_SIGNATURE,         SAMLClientRepresentation::setSignAssertions, (model, value) -> setBooleanAttribute(model, SAML_ASSERTION_SIGNATURE, value)));
        map.put("clientSignatureRequired",      protocolBoolAttr  ("clientSignatureRequired",      SAML_CLIENT_SIGNATURE,            SAMLClientRepresentation::setClientSignatureRequired, (model, value) -> setBooleanAttribute(model, SAML_CLIENT_SIGNATURE, value)));
        map.put("signatureAlgorithm",           protocolStringAttr("signatureAlgorithm",           SAML_SIGNATURE_ALGORITHM,         (rep, v) -> rep.setSignatureAlgorithm(SAMLClientRepresentation.SignatureAlgorithm.fromJson(v)), (model, value) -> setAttribute(model, SAML_SIGNATURE_ALGORITHM, value != null ? ((SAMLClientRepresentation.SignatureAlgorithm) value).name() : null)));
        map.put("signatureCanonicalizationMethod", protocolStringAttr("signatureCanonicalizationMethod", SAML_SIGNATURE_CANONICALIZATION, SAMLClientRepresentation::setSignatureCanonicalizationMethod, (model, value) -> setAttribute(model, SAML_SIGNATURE_CANONICALIZATION, (String) value)));
        map.put("signingCertificate",           protocolStringAttr("signingCertificate",           SAML_SIGNING_CERTIFICATE,         SAMLClientRepresentation::setSigningCertificate, (model, value) -> setAttribute(model, SAML_SIGNING_CERTIFICATE, (String) value)));
        map.put("forcePostBinding",             protocolBoolAttr  ("forcePostBinding",             SAML_FORCE_POST_BINDING,          SAMLClientRepresentation::setForcePostBinding, (model, value) -> setBooleanAttribute(model, SAML_FORCE_POST_BINDING, value)));
        map.put("frontChannelLogout",           protocolBoolAttr  ("frontChannelLogout",           "frontchannelLogout",             SAMLClientRepresentation::setFrontChannelLogout, (model, logout) -> model.setFrontchannelLogout(Boolean.TRUE.equals(logout))));
        map.put("allowEcpFlow",                 protocolBoolAttr  ("allowEcpFlow",                 SAML_ALLOW_ECP_FLOW,              SAMLClientRepresentation::setAllowEcpFlow, (model, value) -> setBooleanAttribute(model, SAML_ALLOW_ECP_FLOW, value)));
    }

    @Override
    protected Object getAttributeValue(ClientModel model, String name) {
        return switch (name) {
            case "nameIdFormat"                  -> model.getAttribute(SAML_NAME_ID_FORMAT);
            case "forceNameIdFormat"             -> getBooleanAttribute(model, SAML_FORCE_NAME_ID_FORMAT);
            case "includeAuthnStatement"         -> getBooleanAttribute(model, SAML_AUTHN_STATEMENT);
            case "signDocuments"                 -> getBooleanAttribute(model, SAML_SERVER_SIGNATURE);
            case "signAssertions"                -> getBooleanAttribute(model, SAML_ASSERTION_SIGNATURE);
            case "clientSignatureRequired"       -> getBooleanAttribute(model, SAML_CLIENT_SIGNATURE);
            case "signatureAlgorithm"            -> model.getAttribute(SAML_SIGNATURE_ALGORITHM);
            case "signatureCanonicalizationMethod" -> model.getAttribute(SAML_SIGNATURE_CANONICALIZATION);
            case "signingCertificate"            -> model.getAttribute(SAML_SIGNING_CERTIFICATE);
            case "forcePostBinding"              -> getBooleanAttribute(model, SAML_FORCE_POST_BINDING);
            case "frontChannelLogout"            -> model.isFrontchannelLogout();
            case "allowEcpFlow"                  -> getBooleanAttribute(model, SAML_ALLOW_ECP_FLOW);
            default                              -> super.getAttributeValue(model, name);
        };
    }

    @Override
    public Object getRepresentationValue(SAMLClientRepresentation rep, String name) {
        return switch (name) {
            case "nameIdFormat"                     -> rep.getNameIdFormat();
            case "forceNameIdFormat"                -> rep.getForceNameIdFormat();
            case "includeAuthnStatement"            -> rep.getIncludeAuthnStatement();
            case "signDocuments"                    -> rep.getSignDocuments();
            case "signAssertions"                   -> rep.getSignAssertions();
            case "clientSignatureRequired"          -> rep.getClientSignatureRequired();
            case "signatureAlgorithm"               -> rep.getSignatureAlgorithm();
            case "signatureCanonicalizationMethod"  -> rep.getSignatureCanonicalizationMethod();
            case "signingCertificate"               -> rep.getSigningCertificate();
            case "forcePostBinding"                 -> rep.getForcePostBinding();
            case "frontChannelLogout"               -> rep.getFrontChannelLogout();
            case "allowEcpFlow"                     -> rep.getAllowEcpFlow();
            default                                 -> super.getRepresentationValue(rep, name);
        };
    }

    private Boolean getBooleanAttribute(ClientModel model, String key) {
        String value = model.getAttribute(key);
        return value != null ? Boolean.parseBoolean(value) : null;
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
