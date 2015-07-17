package org.keycloak.protocol.saml;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAMLRequestParser {
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    public static SAMLDocumentHolder parseRequestRedirectBinding(String samlMessage) {
        InputStream is;
        is = RedirectBindingUtil.base64DeflateDecode(samlMessage);
        SAML2Request saml2Request = new SAML2Request();
        try {
            saml2Request.getSAML2ObjectFromStream(is);
            return saml2Request.getSamlDocumentHolder();
        } catch (Exception e) {
            logger.samlBase64DecodingError(e);
        }
        return null;

    }

    public static SAMLDocumentHolder parseRequestPostBinding(String samlMessage) {
        InputStream is;
        byte[] samlBytes = PostBindingUtil.base64Decode(samlMessage);
        is = new ByteArrayInputStream(samlBytes);
        SAML2Request saml2Request = new SAML2Request();
        try {
            saml2Request.getSAML2ObjectFromStream(is);
            return saml2Request.getSamlDocumentHolder();
        } catch (Exception e) {
            logger.samlBase64DecodingError(e);
        }
        return null;
    }

    public static SAMLDocumentHolder parseResponsePostBinding(String samlMessage) {
        byte[] samlBytes = PostBindingUtil.base64Decode(samlMessage);
        return parseResponseDocument(samlBytes);
    }

    public static SAMLDocumentHolder parseResponseDocument(byte[] samlBytes) {
        InputStream is = new ByteArrayInputStream(samlBytes);
        SAML2Response response = new SAML2Response();
        try {
            response.getSAML2ObjectFromStream(is);
            return response.getSamlDocumentHolder();
        } catch (Exception e) {
            logger.samlBase64DecodingError(e);
        }
        return null;
    }

    public static SAMLDocumentHolder parseResponseRedirectBinding(String samlMessage) {
        InputStream is = RedirectBindingUtil.base64DeflateDecode(samlMessage);
        SAML2Response response = new SAML2Response();
        try {
            response.getSAML2ObjectFromStream(is);
            return response.getSamlDocumentHolder();
        } catch (Exception e) {
            logger.samlBase64DecodingError(e);
        }
        return null;

    }


}
