package org.keycloak.protocol.saml;

import org.keycloak.VerificationException;
import org.keycloak.models.ClientModel;
import org.keycloak.util.PemUtils;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.w3c.dom.Document;

import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolUtils {

    public static void verifyDocumentSignature(ClientModel client, Document document) throws VerificationException {
        if (!"true".equals(client.getAttribute(SamlProtocol.SAML_CLIENT_SIGNATURE_ATTRIBUTE))) {
            return;
        }
        SAML2Signature saml2Signature = new SAML2Signature();
        PublicKey publicKey = getSignatureValidationKey(client);
        try {
            if (!saml2Signature.validate(document, publicKey)) {
                throw new VerificationException("Invalid signature on document");
            }
        } catch (ProcessingException e) {
            throw new VerificationException("Error validating signature", e);
        }
    }

    public static PublicKey getSignatureValidationKey(ClientModel client) throws VerificationException {
        return getPublicKey(client, SamlProtocol.SAML_SIGNING_CERTIFICATE_ATTRIBUTE);
    }

    public static PublicKey getEncryptionValidationKey(ClientModel client) throws VerificationException {
        return getPublicKey(client, SamlProtocol.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE);
    }

    public static PublicKey getPublicKey(ClientModel client, String attribute) throws VerificationException {
        String certPem = client.getAttribute(attribute);
        if (certPem == null) throw new VerificationException("Client does not have a public key.");
        Certificate cert = null;
        try {
            cert = PemUtils.decodeCertificate(certPem);
        } catch (Exception e) {
            throw new VerificationException("Could not decode cert", e);
        }
        return cert.getPublicKey();
    }


}
