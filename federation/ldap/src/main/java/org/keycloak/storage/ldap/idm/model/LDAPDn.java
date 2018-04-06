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

import javax.naming.ldap.Rdn;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPDn {

    private final Deque<Entry> entries;

    private LDAPDn() {
        this.entries = new LinkedList<>();
    }

    private LDAPDn(Deque<Entry> entries) {
        this.entries = entries;
    }

    public static LDAPDn fromString(String dnString) {
        LDAPDn dn = new LDAPDn();
        
        // In certain OpenLDAP implementations the uniqueMember attribute is mandatory
        // Thus, if a new group is created, it will contain an empty uniqueMember attribute
        // Later on, when adding members, this empty attribute will be kept
        // Keycloak must be able to process it, properly, w/o throwing an ArrayIndexOutOfBoundsException
        if(dnString.trim().isEmpty())
            return dn;
        
        String[] rdns = dnString.split("(?<!\\\\),");
        for (String entryStr : rdns) {
            String[] rdn = entryStr.split("(?<!\\\\)=");
            if (rdn.length >1) {
                dn.addLast(rdn[0].trim(), rdn[1].trim());
            } else {
                dn.addLast(rdn[0].trim(), "");
            }
        }

        return dn;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LDAPDn)) {
            return false;
        }

        return toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return toString(entries);
    }

    private static String toString(Collection<Entry> entries) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (Entry rdn : entries) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append(rdn.attrName).append("=").append(rdn.attrValue);
        }

        return builder.toString();
    }

    /**
     * @return string like "uid=joe" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getFirstRdn() {
        Entry firstEntry = entries.getFirst();
        return firstEntry.attrName + "=" + unescapeValue(firstEntry.attrValue);
    }

    /**
     * @return string attribute name like "uid" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getFirstRdnAttrName() {
        Entry firstEntry = entries.getFirst();
        return firstEntry.attrName;
    }

    /**
     * @return string attribute value like "joe" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getFirstRdnAttrValue() {
        Entry firstEntry = entries.getFirst();
        String dnEscaped = firstEntry.attrValue;
        return unescapeValue(dnEscaped);
    }

    private String unescapeValue(String escaped) {
        // Something needed to handle non-String types?
        return Rdn.unescapeValue(escaped).toString();
    }

    /**
     *
     * @return DN like "dc=something,dc=org" from the DN like "uid=joe,dc=something,dc=org".
     * Returned DN will be new clone not related to the original DN instance.
     *
     */
    public LDAPDn getParentDn() {
        LinkedList<Entry> parentDnEntries = new LinkedList<>(entries);
        parentDnEntries.remove();
        return new LDAPDn(parentDnEntries);
    }

    public boolean isDescendantOf(LDAPDn expectedParentDn) {
        int parentEntriesCount = expectedParentDn.entries.size();

        Deque<Entry> myEntries = new LinkedList<>(this.entries);
        boolean someRemoved = false;
        while (myEntries.size() > parentEntriesCount) {
            myEntries.removeFirst();
            someRemoved = true;
        }

        String myEntriesParentStr = toString(myEntries).toLowerCase();
        String expectedParentDnStr = expectedParentDn.toString().toLowerCase();
        return someRemoved && myEntriesParentStr.equals(expectedParentDnStr);
    }

    public void addFirst(String rdnName, String rdnValue) {
        rdnValue = Rdn.escapeValue(rdnValue);
        entries.addFirst(new Entry(rdnName, rdnValue));
    }

    private void addLast(String rdnName, String rdnValue) {
        entries.addLast(new Entry(rdnName, rdnValue));
    }

    private static class Entry {
        private final String attrName;
        private final String attrValue;

        private Entry(String attrName, String attrValue) {
            this.attrName = attrName;
            this.attrValue = attrValue;
        }
    }
}
