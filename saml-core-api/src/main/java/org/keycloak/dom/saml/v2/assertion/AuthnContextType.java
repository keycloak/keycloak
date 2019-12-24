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
package org.keycloak.dom.saml.v2.assertion;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>
 * Java class for AuthnContextType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthnContextType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;sequence>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextClassRef"/>
 *             &lt;choice minOccurs="0">
 *               &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDecl"/>
 *               &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDeclRef"/>
 *             &lt;/choice>
 *           &lt;/sequence>
 *           &lt;choice>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDecl"/>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDeclRef"/>
 *           &lt;/choice>
 *         &lt;/choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthenticatingAuthority" maxOccurs="unbounded"
 * minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AuthnContextType implements Serializable {

    private final Set<URI> authenticatingAuthority = new LinkedHashSet<>();

    private AuthnContextTypeSequence sequence;

    private final Set<URIType> URITypes = new HashSet<>();

    /**
     * Add an authenticating authority
     *
     * @param aa {@link URI}
     */
    public void addAuthenticatingAuthority(URI aa) {
        authenticatingAuthority.add(aa);
    }

    /**
     * Add Authenticating Authority
     *
     * @param aas an array of {@link URI}
     */
    public void addAuthenticatingAuthority(URI[] aas) {
        authenticatingAuthority.addAll(Arrays.asList(aas));
    }

    /**
     * Remove an authenticating authority
     *
     * @param aa
     */
    public void removeAuthenticatingAuthority(URI aa) {
        authenticatingAuthority.remove(aa);
    }

    /**
     * Get a read only set of authenticating authority
     *
     * @return
     */
    public Set<URI> getAuthenticatingAuthority() {
        return Collections.unmodifiableSet(authenticatingAuthority);
    }

    /**
     * Get the sequence
     *
     * @return
     */
    public AuthnContextTypeSequence getSequence() {
        return sequence;
    }

    /**
     * Set the authn context sequence
     *
     * @param sequence
     */
    public void setSequence(AuthnContextTypeSequence sequence) {
        this.sequence = sequence;
    }

    /**
     * Add an URI type
     *
     * @param aa
     */
    public void addURIType(URIType aa) {
        URITypes.add(aa);
    }

    /**
     * Add an array of URI Type
     *
     * @param aas
     */
    public void addURIType(URIType[] aas) {
        URITypes.addAll(Arrays.asList(aas));
    }

    /**
     * Get a read only set of URI type
     *
     * @return
     */
    public Set<URIType> getURIType() {
        return Collections.unmodifiableSet(URITypes);
    }

    /**
     * Add an URI type
     *
     * @param aa
     */
    public void removeURIType(URIType aa) {
        URITypes.remove(aa);
    }

    /**
     * <sequence> <element ref="saml:AuthnContextClassRef"/> <choice minOccurs="0"> <element
     * ref="saml:AuthnContextDecl"/>
     * <element ref="saml:AuthnContextDeclRef"/> </choice> </sequence>
     */
    public class AuthnContextTypeSequence implements Serializable {

        private AuthnContextClassRefType classRef;

        private AuthnContextDeclType authnContextDecl;

        private final Set<URIType> URITypes = new HashSet<URIType>();

        public AuthnContextClassRefType getClassRef() {
            return classRef;
        }

        public void setClassRef(AuthnContextClassRefType classRef) {
            this.classRef = classRef;
        }

        public void addURIType(URIType aa) {
            URITypes.add(aa);
        }

        public void removeURIType(URIType aa) {
            URITypes.remove(aa);
        }

        public void addURIType(URIType[] aas) {
            URITypes.addAll(Arrays.asList(aas));
        }

        public Set<URIType> getURIType() {
            return Collections.unmodifiableSet(URITypes);
        }

        public AuthnContextDeclType getAuthnContextDecl() {
            return authnContextDecl;
        }

        public void setAuthnContextDecl(AuthnContextDeclType authnContextDecl) {
            this.authnContextDecl = authnContextDecl;
        }
    }
}