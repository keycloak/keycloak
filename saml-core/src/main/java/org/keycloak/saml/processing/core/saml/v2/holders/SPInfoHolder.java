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

/**
 * <p>
 * Holds the information about a Service Provider
 * </p>
 * <p>
 * This holder is useful in generating saml messages
 * </p>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 10, 2008
 */
public class SPInfoHolder {

    private String requestID;
    private String responseDestinationURI;
    private String issuer;

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getResponseDestinationURI() {
        return responseDestinationURI;
    }

    public void setResponseDestinationURI(String responseDestinationURI) {
        this.responseDestinationURI = responseDestinationURI;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}