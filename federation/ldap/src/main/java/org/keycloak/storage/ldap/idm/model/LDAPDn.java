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

package org.keycloak.storage.ldap.idm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPDn {

    private final LdapName ldapName;

    private LDAPDn() {
        this.ldapName = new LdapName(Collections.emptyList());
    }

    private LDAPDn(LdapName ldapName) {
        this.ldapName = ldapName;
    }

    public static LDAPDn fromLdapName(LdapName ldapName) {
        return new LDAPDn(ldapName);
    }

    public static LDAPDn fromString(String dnString) {
        // In certain OpenLDAP implementations the uniqueMember attribute is mandatory
        // Thus, if a new group is created, it will contain an empty uniqueMember attribute
        // Later on, when adding members, this empty attribute will be kept
        // Keycloak must be able to process it, properly, w/o throwing an ArrayIndexOutOfBoundsException
        if(dnString.trim().isEmpty()) {
            return new LDAPDn();
        }

        try {
            return new LDAPDn(new LdapName(dnString));
        } catch (NamingException e) {
            throw new IllegalArgumentException("Invalid DN:" + dnString, e);
        }
    }

    public LdapName getLdapName() {
        return ldapName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LDAPDn)) {
            return false;
        }

        return ldapName.equals(((LDAPDn) obj).ldapName);
    }

    @Override
    public int hashCode() {
        return ldapName.hashCode();
    }

    @Override
    public String toString() {
        return ldapName.toString();
    }

    /**
     * @return first entry. Usually entry corresponding to something like "uid=joe" from the DN like "uid=joe,dc=something,dc=org"
     */
    public RDN getFirstRdn() {
        if (ldapName.size() > 0) {
            return new RDN(ldapName.getRdn(ldapName.size() - 1));
        }
        return null;
    }

    /**
     *
     * @return DN like "dc=something,dc=org" from the DN like "uid=joe,dc=something,dc=org".
     * Returned DN will be new clone not related to the original DN instance.
     *
     */
    public LDAPDn getParentDn() {
        if (ldapName.size() > 0) {
            LdapName parent = (LdapName) ldapName.getPrefix(ldapName.size() - 1);
            return new LDAPDn(parent);
        }
        return null;
    }

    public boolean isDescendantOf(LDAPDn expectedParentDn) {
        LDAPDn parent = getParentDn();
        if (parent == null) {
            return false;
        }
        return parent.ldapName.startsWith(expectedParentDn.ldapName);
    }

    public void addFirst(String rdnName, String rdnValue) {
        try {
            ldapName.add(rdnName + "=" + Rdn.escapeValue(rdnValue));
        } catch (NamingException e) {
            throw new IllegalArgumentException("Invalid RDN name=" + rdnName + " value=" + rdnValue, e);
        }
    }

    public void addFirst(RDN entry) {
        ldapName.add(entry.rdn);
    }

    /**
     * Single RDN inside the DN. RDN usually consists of single item like "uid=john" . In some rare cases, it can have multiple
     * sub-entries like "uid=john+sn=Doe"
     */
    public static class RDN {

        private Rdn rdn;

        private RDN(Rdn rdn) {
            this.rdn = rdn;
        }

        /**
         * @return Keys in the RDN. Returned list is the copy, which is not linked to the original RDN
         */
        public List<String> getAllKeys() {
            try {
                Attributes attrs = rdn.toAttributes();
                List<String> result = new ArrayList<>(attrs.size());
                NamingEnumeration<? extends Attribute> ne = attrs.getAll();
                while (ne.hasMore()) {
                    result.add(ne.next().getID());
                }
                return result;
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Assume that RDN is something like "uid=john", then this method will return "john" in case that attrName is "uid" .
         * This is useful in case that RDN is multi-key - something like "uid=john+cn=John Doe" and we want to return just "john" as the value of "uid"
         *
         * The returned value will be unescaped
         *
         * @param attrName
         * @return
         */
        public String getAttrValue(String attrName) {
            try {
                Attribute attr = rdn.toAttributes().get(attrName);
                if (attr != null) {
                    Object value = attr.get();
                    if (value != null) {
                        return value.toString();
                    }
                }
                return null;
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }

        public void setAttrValue(String attrName, String newAttrValue) {
            try {
                Attributes attrs = rdn.toAttributes();
                Attribute attr = attrs.get(attrName);
                if (attr != null) {
                    attr.clear();
                    attr.add(newAttrValue);
                } else {
                    attrs.put(attrName, newAttrValue);
                }
                rdn = new Rdn(attrs);
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }

        public boolean removeAttrValue(String attrName) {
            try {
                Attributes attrs = rdn.toAttributes();
                if (attrs.remove(attrName) != null) {
                    rdn = new Rdn(attrs);
                    return true;
                }
                return false;
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String toString() {
            return rdn.toString();
        }

        /**
         *
         * @param escaped indicates whether return escaped or unescaped values. EG. "uid=john,comma" VS "uid=john\,comma"
         * @return
         */
        public String toString(boolean escaped) {
            if (escaped) {
                return toString();
            }

            StringBuilder builder = new StringBuilder();
            try {
                NamingEnumeration<? extends Attribute> attrs = rdn.toAttributes().getAll();
                while (attrs.hasMore()) {
                    Attribute attr = attrs.next();
                    NamingEnumeration<?> values = attr.getAll();
                    while (values.hasMore()) {
                        builder.append(attr.getID()).append("=").append(values.next().toString()).append("+");
                    }
                }
                builder.setLength(builder.length() - 1);
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }

            return builder.toString();
        }
    }
}
