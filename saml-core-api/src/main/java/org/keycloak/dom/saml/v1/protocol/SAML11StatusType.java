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
package org.keycloak.dom.saml.v1.protocol;

import org.keycloak.dom.saml.common.CommonStatusDetailType;

import java.io.Serializable;

/**
 * <complexType name="StatusType"> <sequence> <element ref="samlp:StatusCode"/> <element ref="samlp:StatusMessage"
 * minOccurs="0"/> <element ref="samlp:StatusDetail" minOccurs="0"/> </sequence>
 *
 * </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11StatusType implements Serializable {

    protected SAML11StatusCodeType statusCode;

    protected String statusMessage;

    protected CommonStatusDetailType statusDetail;

    public SAML11StatusCodeType getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(SAML11StatusCodeType statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public CommonStatusDetailType getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(CommonStatusDetailType statusDetail) {
        this.statusDetail = statusDetail;
    }

    public static SAML11StatusType successType() {
        SAML11StatusType success = new SAML11StatusType();
        success.setStatusCode(SAML11StatusCodeType.SUCCESS);
        return success;
    }
}