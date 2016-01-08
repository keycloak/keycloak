package org.keycloak.protocol.saml;

import org.keycloak.representations.idm.ClientRepresentation;

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

    public String getSamlServerSignature() {
        if (getAttributes() == null) return null;
        return getAttributes().get(SamlConfigAttributes.SAML_SERVER_SIGNATURE);

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
