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
package org.keycloak.saml.processing.core.saml.v2.holders;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

/**
 * Holds info about the issuer for saml messages creation
 *
 * @param <JBossSAMLConstants>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 10, 2008
 */
public class IssuerInfoHolder {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private NameIDType issuer;

    private String statusCodeURI = JBossSAMLURIConstants.STATUS_SUCCESS.get();

    private String samlVersion = JBossSAMLConstants.VERSION_2_0.get();

    public IssuerInfoHolder(NameIDType issuer) {
        if (issuer == null)
            throw logger.nullArgumentError("issuer");
        this.issuer = issuer;
    }

    public IssuerInfoHolder(String issuerAsString) {
        if (issuerAsString == null)
            throw logger.nullArgumentError("issuerAsString");
        issuer = new NameIDType();
        issuer.setValue(issuerAsString);
    }

    public NameIDType getIssuer() {
        return issuer;
    }

    public void setIssuer(NameIDType issuer) {
        this.issuer = issuer;
    }

    public String getStatusCode() {
        return statusCodeURI;
    }

    public void setStatusCode(String statusCode) {
        this.statusCodeURI = statusCode;
    }

    public String getSamlVersion() {
        return samlVersion;
    }

    public void setSamlVersion(String samlVersion) {
        this.samlVersion = samlVersion;
    }
}