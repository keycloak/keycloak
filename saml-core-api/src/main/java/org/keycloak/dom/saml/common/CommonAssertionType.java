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
package org.keycloak.dom.saml.common;

import java.io.Serializable;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * SAML AssertionType
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 21, 2011
 */
public class CommonAssertionType implements Serializable {

    protected XMLGregorianCalendar issueInstant;

    protected String ID;

    public CommonAssertionType(String iD, XMLGregorianCalendar issueInstant) {
        if (iD == null)
            throw new IllegalArgumentException("iD is null");
        if (issueInstant == null)
            throw new IllegalArgumentException("issueInstant is null");

        this.issueInstant = issueInstant;
        ID = iD;
    }

    public XMLGregorianCalendar getIssueInstant() {
        return issueInstant;
    }

    public String getID() {
        return ID;
    }
}