package org.keycloak.protocol.saml;

import org.keycloak.VerificationException;
import org.keycloak.models.ClientModel;
import org.keycloak.util.PemUtils;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolUtils {

    public static void verifyDocumentSignature(ClientModel client, Document document) throws VerificationException {
        if (!"true".equals(client.getAttribute("saml.client.signature"))) {
            return;
        }
        SAML2Signature saml2Signature = new SAML2Signature();
        PublicKey publicKey = getPublicKey(client);
        try {
            if (!saml2Signature.validate(document, publicKey)) {
                throw new VerificationException("Invalid signature on document");
            }
        } catch (ProcessingException e) {
            throw new VerificationException("Error validating signature", e);
        }
    }

    public static PublicKey getPublicKey(ClientModel client) throws VerificationException {
        String publicKeyPem = client.getAttribute(ClientModel.PUBLIC_KEY);
        if (publicKeyPem == null) throw new VerificationException("Client does not have a public key.");
        PublicKey publicKey = null;
        try {
            publicKey = PemUtils.decodePublicKey(publicKeyPem);
        } catch (Exception e) {
            throw new VerificationException("Could not decode public key", e);
        }
        return publicKey;
    }

    public static void signDocument(Document samlDocument, KeyPair signingKeyPair, String signatureMethod, String signatureDigestMethod, X509Certificate signingCertificate) throws ProcessingException {
        SAML2Signature samlSignature = new SAML2Signature();

        if (signatureMethod != null) {
            samlSignature.setSignatureMethod(signatureMethod);
        }

        if (signatureDigestMethod != null) {
            samlSignature.setDigestMethod(signatureDigestMethod);
        }

        Node nextSibling = samlSignature.getNextSiblingOfIssuer(samlDocument);

        samlSignature.setNextSibling(nextSibling);

        if (signingCertificate != null) {
            samlSignature.setX509Certificate(signingCertificate);
        }

        samlSignature.signSAMLDocument(samlDocument, signingKeyPair);
    }


}
