package org.keycloak.protocol.saml;

import org.keycloak.VerificationException;
import org.keycloak.models.ClientModel;
import org.keycloak.util.PemUtils;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.picketlink.identity.federation.web.util.RedirectBindingUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.PublicKey;
import java.security.Signature;
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
        PublicKey publicKey = getSignatureValidationKey(client);
        verifyDocumentSignature(document, publicKey);
    }

    public static void verifyDocumentSignature(Document document, PublicKey publicKey) throws VerificationException {
        SAML2Signature saml2Signature = new SAML2Signature();
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

    public static void verifyRedirectSignature(PublicKey publicKey, UriInfo uriInformation) throws VerificationException {
        MultivaluedMap<String, String> encodedParams = uriInformation.getQueryParameters(false);
        String request = encodedParams.getFirst(GeneralConstants.SAML_REQUEST_KEY);
        String algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
        String signature = encodedParams.getFirst(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);
        String decodedAlgorithm = uriInformation.getQueryParameters(true).getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);

        if (request == null) throw new VerificationException("SAMLRequest as null");
        if (algorithm == null) throw new VerificationException("SigAlg as null");
        if (signature == null) throw new VerificationException("Signature as null");

        // Shibboleth doesn't sign the document for redirect binding.
        // todo maybe a flag?


        UriBuilder builder = UriBuilder.fromPath("/")
                .queryParam(GeneralConstants.SAML_REQUEST_KEY, request);
        if (encodedParams.containsKey(GeneralConstants.RELAY_STATE)) {
            builder.queryParam(GeneralConstants.RELAY_STATE, encodedParams.getFirst(GeneralConstants.RELAY_STATE));
        }
        builder.queryParam(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY, algorithm);
        String rawQuery = builder.build().getRawQuery();

        try {
            byte[] decodedSignature = RedirectBindingUtil.urlBase64Decode(signature);

            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getFromXmlMethod(decodedAlgorithm);
            Signature validator = signatureAlgorithm.createSignature(); // todo plugin signature alg
            validator.initVerify(publicKey);
            validator.update(rawQuery.getBytes("UTF-8"));
            if (!validator.verify(decodedSignature)) {
                throw new VerificationException("Invalid query param signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }


}
