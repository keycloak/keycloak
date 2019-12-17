/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.saml;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Document;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2LogoutRequestBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2LogoutRequestBuilder> {
    protected NameIDType nameId;
    protected String sessionIndex;
    protected long assertionExpiration;
    protected String destination;
    protected String issuer;
    protected final List<NodeGenerator> extensions = new LinkedList<>();

    public SAML2LogoutRequestBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2LogoutRequestBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    @Override
    public SAML2LogoutRequestBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

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

    /**
     *
     * @param userPrincipal
     * @param userPrincipalFormat
     * @return
     * @deprecated Prefer {@link #nameId(org.keycloak.dom.saml.v2.assertion.NameIDType)}
     */
    @Deprecated
    public SAML2LogoutRequestBuilder userPrincipal(String userPrincipal, String userPrincipalFormat) {
        NameIDType nid = new NameIDType();
        nid.setValue(userPrincipal);
        if (userPrincipalFormat != null) {
            nid.setFormat(URI.create(userPrincipalFormat));
        }
        
        return nameId(nid);
    }

    public SAML2LogoutRequestBuilder nameId(NameIDType nameId) {
        this.nameId = nameId;
        return this;
    }

    public SAML2LogoutRequestBuilder sessionIndex(String index) {
        this.sessionIndex = index;
        return this;
    }

    public Document buildDocument() throws ProcessingException, ConfigurationException, ParsingException {
        Document document = SAML2Request.convert(createLogoutRequest());
        return document;
    }

    public LogoutRequestType createLogoutRequest() throws ConfigurationException {
        LogoutRequestType lort = SAML2Request.createLogoutRequest(issuer);

        lort.setNameID(nameId);

        if (issuer != null) {
            NameIDType issuerID = new NameIDType();
            issuerID.setValue(issuer);
            lort.setIssuer(issuerID);
        }
        if (sessionIndex != null) lort.addSessionIndex(sessionIndex);


        if (assertionExpiration > 0) lort.setNotOnOrAfter(XMLTimeUtil.add(lort.getIssueInstant(), assertionExpiration * 1000));
        lort.setDestination(URI.create(destination));

        if (! this.extensions.isEmpty()) {
            ExtensionsType extensionsType = new ExtensionsType();
            for (NodeGenerator extension : this.extensions) {
                extensionsType.addExtension(extension);
            }
            lort.setExtensions(extensionsType);
        }

        return lort;
    }
}
