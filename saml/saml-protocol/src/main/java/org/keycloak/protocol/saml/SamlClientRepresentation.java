package org.keycloak.protocol.saml;

import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlClientRepresentation {
    protected ClientRepresentation rep;

    public SamlClientRepresentation(ClientRepresentation rep) {
        this.rep = rep;
    }

    public String getCanonicalizationMethod() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE);
    }

    public String getSignatureAlgorithm() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM);
    }

    public String getNameIDFormat() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE);

    }

    public String getIncludeAuthnStatement() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_AUTHNSTATEMENT);

    }

    public String getForceNameIDFormat() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE);
    }

    public String getSamlServerSignature() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_SERVER_SIGNATURE);

    }

    public String getForcePostBinding() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_FORCE_POST_BINDING);

    }
    public String getClientSignature() {
        if (rep.getAttributes() == null) return null;
        return rep.getAttributes().get(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE);

    }
}
