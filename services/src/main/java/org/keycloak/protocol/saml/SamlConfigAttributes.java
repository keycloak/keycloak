/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.saml;

import org.keycloak.services.util.CertificateInfoHelper;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlConfigAttributes {
    String SAML_SIGNING_PRIVATE_KEY = "saml.signing.private.key";
    String SAML_CANONICALIZATION_METHOD_ATTRIBUTE = "saml_signature_canonicalization_method";
    String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    String SAML_NAME_ID_FORMAT_ATTRIBUTE = "saml_name_id_format";
    String SAML_AUTHNSTATEMENT = "saml.authnstatement";
    String SAML_ONETIMEUSE_CONDITION = "saml.onetimeuse.condition";
    String SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE = "saml_force_name_id_format";
    String SAML_ARTIFACT_BINDING = "saml.artifact.binding";
    String SAML_SERVER_SIGNATURE = "saml.server.signature";
    String SAML_SERVER_SIGNATURE_KEYINFO_EXT = "saml.server.signature.keyinfo.ext";
    String SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER = "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer";
    String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    String SAML_ENCRYPT = "saml.encrypt";
    String SAML_CLIENT_SIGNATURE_ATTRIBUTE = "saml.client.signature";
    String SAML_SIGNING_CERTIFICATE_ATTRIBUTE = "saml.signing." + CertificateInfoHelper.X509CERTIFICATE;
    String SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE = "saml.encryption." + CertificateInfoHelper.X509CERTIFICATE;
    String SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE = "saml.encryption." + CertificateInfoHelper.PRIVATE_KEY;
    String SAML_ASSERTION_LIFESPAN = "saml.assertion.lifespan";
    String SAML_ARTIFACT_BINDING_IDENTIFIER = "saml.artifact.binding.identifier";
}
