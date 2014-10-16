package org.keycloak.protocol.saml;

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.core.saml.v2.common.SAMLDocumentHolder;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.picketlink.identity.federation.web.util.RedirectBindingUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAMLRequestParser {
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    public static SAMLDocumentHolder parseRedirectBinding(String samlMessage) {
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

    public static SAMLDocumentHolder parsePostBinding(String samlMessage) {
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
}
