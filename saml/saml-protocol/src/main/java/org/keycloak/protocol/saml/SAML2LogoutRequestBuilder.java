package org.keycloak.protocol.saml;

import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.core.saml.v2.util.DocumentUtil;
import org.picketlink.identity.federation.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.core.sts.PicketLinkCoreSTS;
import org.picketlink.identity.federation.saml.v2.assertion.NameIDType;
import org.picketlink.identity.federation.saml.v2.protocol.LogoutRequestType;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.w3c.dom.Document;

import java.io.IOException;
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

    public RedirectBindingBuilder redirectBinding()  throws ConfigurationException, ProcessingException, ParsingException {
        Document samlResponseDocument = buildDocument();
        return new RedirectBindingBuilder(samlResponseDocument);

    }

    public PostBindingBuilder postBinding()  throws ConfigurationException, ProcessingException, ParsingException {
        Document samlResponseDocument = buildDocument();
        return new PostBindingBuilder(samlResponseDocument);

    }
    public Document buildDocument() throws ProcessingException, ConfigurationException, ParsingException {
        Document document = new SAML2Request().convert(createLogoutRequest());
        if (encrypt) encryptDocument(document);
        return document;
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
