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

import java.net.URI;

/**
 * <complexType name="AuthenticationQueryType"> <complexContent> <extension base="samlp:SubjectQueryAbstractType">
 * <attribute
 * name="AuthenticationMethod" type="anyURI"/> </extension>
 *
 * </complexContent> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AuthenticationQueryType extends SAML11SubjectQueryAbstractType {

    protected URI authenticationMethod;

    public URI getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(URI authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }
}