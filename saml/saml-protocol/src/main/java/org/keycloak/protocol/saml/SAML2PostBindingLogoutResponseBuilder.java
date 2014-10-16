package org.keycloak.protocol.saml;

import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.core.saml.v2.common.IDGenerator;
import org.picketlink.identity.federation.core.saml.v2.factories.JBossSAMLAuthnResponseFactory;
import org.picketlink.identity.federation.core.saml.v2.holders.IDPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.IssuerInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.SPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.util.DocumentUtil;
import org.picketlink.identity.federation.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.core.sts.PicketLinkCoreSTS;
import org.picketlink.identity.federation.saml.v2.assertion.NameIDType;
import org.picketlink.identity.federation.saml.v2.protocol.LogoutRequestType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2PostBindingLogoutResponseBuilder extends SAML2PostBindingBuilder<SAML2PostBindingLogoutResponseBuilder> {
    protected String userPrincipal;

    public SAML2PostBindingLogoutResponseBuilder userPrincipal(String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    public String buildRequestString() {
        try {
            Document logoutRequestDocument = new SAML2Request().convert(createLogoutRequest());
            if (signed) {
                signDocument(logoutRequestDocument);
            }
            byte[] responseBytes = DocumentUtil.getDocumentAsString(logoutRequestDocument).getBytes("UTF-8");
            return PostBindingUtil.base64Encode(new String(responseBytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LogoutRequestType createLogoutRequest() throws ConfigurationException {
        LogoutRequestType lort = new SAML2Request().createLogoutRequest(responseIssuer);

        NameIDType nameID = new NameIDType();
        nameID.setValue(userPrincipal);
        //Deal with NameID Format
        String nameIDFormat = JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
        nameID.setFormat(URI.create(nameIDFormat));
        lort.setNameID(nameID);

        long assertionValidity = PicketLinkCoreSTS.instance().getConfiguration().getIssuedTokenTimeout();

        lort.setNotOnOrAfter(XMLTimeUtil.add(lort.getIssueInstant(), assertionValidity));
        lort.setDestination(URI.create(destination));
        return lort;
    }
}
