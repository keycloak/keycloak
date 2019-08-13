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

import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlRepresentationAttributes {
    protected Map<String, String> attributes;

    public SamlRepresentationAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getCanonicalizationMethod() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE);
    }

    protected Map<String, String> getAttributes() {
        return attributes;
    }

    public String getSignatureAlgorithm() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM);
    }

    public String getNameIDFormat() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE);

    }

    public String getIncludeAuthnStatement() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_AUTHNSTATEMENT);

    }

    public String getForceNameIDFormat() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE);
    }

    public String getSamlArtifactBinding() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_ARTIFACT_BINDING);
    }

    public String getSamlServerSignature() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_SERVER_SIGNATURE);
    }

    public String getAddExtensionsElementWithKeyInfo() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_EXT);
    }

    public String getForcePostBinding() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_FORCE_POST_BINDING);

    }
    public String getClientSignature() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE);

    }
}
