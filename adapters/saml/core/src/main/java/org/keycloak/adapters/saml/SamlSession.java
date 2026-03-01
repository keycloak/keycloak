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

package org.keycloak.adapters.saml;

import java.io.Serializable;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.adapters.spi.KeycloakAccount;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlSession implements Serializable, KeycloakAccount {
    private SamlPrincipal principal;
    private Set<String> roles;
    private String sessionIndex;
    private XMLGregorianCalendar sessionNotOnOrAfter;

    public SamlSession() {
    }

    public SamlSession(SamlPrincipal principal, Set<String> roles, String sessionIndex, XMLGregorianCalendar sessionNotOnOrAfter) {
        this.principal = principal;
        this.roles = roles;
        this.sessionIndex = sessionIndex;
        this.sessionNotOnOrAfter = sessionNotOnOrAfter;
    }

    public SamlPrincipal getPrincipal() {
        return principal;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }

    public XMLGregorianCalendar getSessionNotOnOrAfter() {
        return sessionNotOnOrAfter;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof SamlSession))
            return false;

        SamlSession otherSession = (SamlSession) other;

        return (this.principal != null ? this.principal.equals(otherSession.principal) : otherSession.principal == null) &&
                (this.roles != null ? this.roles.equals(otherSession.roles) : otherSession.roles == null) &&
                (this.sessionIndex != null ? this.sessionIndex.equals(otherSession.sessionIndex) : otherSession.sessionIndex == null);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.principal == null ? 0 : this.principal.hashCode());
        result = prime * result + (this.roles == null ? 0 : this.roles.hashCode());
        result = prime * result + (this.sessionIndex == null ? 0 : this.sessionIndex.hashCode());
        return result;
    }
}
