package org.keycloak.protocol.saml;

import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.core.saml.v2.util.DocumentUtil;
import org.picketlink.identity.federation.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.core.sts.PicketLinkCoreSTS;
import org.picketlink.identity.federation.saml.v2.assertion.NameIDType;
import org.picketlink.identity.federation.saml.v2.protocol.LogoutRequestType;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.w3c.dom.Document;

import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2LogoutRequestBuilder extends SAML2BindingBuilder<SAML2LogoutRequestBuilder> {
    protected String userPrincipal;

    public SAML2LogoutRequestBuilder userPrincipal(String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    public String buildRequestString() {
        try {
            Document logoutRequestDocument = new SAML2Request().convert(createLogoutRequest());
            encryptAndSign(logoutRequestDocument);
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
