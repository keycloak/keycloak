package org.keycloak.protocol.saml;

import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.w3c.dom.Document;

import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2LogoutRequestBuilder extends SAML2BindingBuilder<SAML2LogoutRequestBuilder> {
    protected String userPrincipal;
    protected String userPrincipalFormat;
    protected String sessionIndex;
    protected long assertionExpiration;

    /**
     * Length of time in seconds the assertion is valid for
     * See SAML core specification 2.5.1.2 NotOnOrAfter
     *
     * @param assertionExpiration Number of seconds the assertion should be valid
     * @return
     */
    public SAML2LogoutRequestBuilder assertionExpiration(int assertionExpiration) {
        this.assertionExpiration = assertionExpiration;
        return this;
    }


    public SAML2LogoutRequestBuilder userPrincipal(String nameID, String nameIDformat) {
        this.userPrincipal = nameID;
        this.userPrincipalFormat = nameIDformat;
        return this;
    }

    public SAML2LogoutRequestBuilder sessionIndex(String index) {
        this.sessionIndex = index;
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
        LogoutRequestType lort = new SAML2Request().createLogoutRequest(issuer);

        NameIDType nameID = new NameIDType();
        nameID.setValue(userPrincipal);
        //Deal with NameID Format
        String nameIDFormat = userPrincipalFormat;
        nameID.setFormat(URI.create(nameIDFormat));
        lort.setNameID(nameID);

        if (issuer != null) {
            NameIDType issuerID = new NameIDType();
            issuerID.setValue(issuer);
            lort.setIssuer(issuerID);
        }
        if (sessionIndex != null) lort.addSessionIndex(sessionIndex);


        if (assertionExpiration > 0) lort.setNotOnOrAfter(XMLTimeUtil.add(lort.getIssueInstant(), assertionExpiration * 1000));
        lort.setDestination(URI.create(destination));
        return lort;
    }
}
