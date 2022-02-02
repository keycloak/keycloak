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

package org.keycloak.models.map.storage.ldap.model;

import javax.naming.ldap.Rdn;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LdapMapDn {
    
    private static final Pattern DN_PATTERN = Pattern.compile("(?<!\\\\),");
    private static final Pattern ENTRY_PATTERN = Pattern.compile("(?<!\\\\)\\+");
    private static final Pattern SUB_ENTRY_PATTERN = Pattern.compile("(?<!\\\\)=");

    private final Deque<RDN> entries;

    private LdapMapDn() {
        this.entries = new LinkedList<>();
    }

    private LdapMapDn(Deque<RDN> entries) {
        this.entries = entries;
    }

    public static LdapMapDn fromString(String dnString) {
        LdapMapDn dn = new LdapMapDn();
        
        // In certain OpenLDAP implementations the uniqueMember attribute is mandatory
        // Thus, if a new group is created, it will contain an empty uniqueMember attribute
        // Later on, when adding members, this empty attribute will be kept
        // Keycloak must be able to process it, properly, w/o throwing an ArrayIndexOutOfBoundsException
        if(dnString.trim().isEmpty())
            return dn;

        String[] rdns = DN_PATTERN.split(dnString);
        for (String entryStr : rdns) {
            if (entryStr.indexOf('+') == -1) {
                // This is 99.9% of cases where RDN consists of single key-value pair
                SubEntry subEntry = parseSingleSubEntry(dn, entryStr);
                dn.addLast(new RDN(subEntry));
            } else {
                // This is 0.1% of cases where RDN consists of more key-value pairs like "uid=foo+cn=bar"
                String[] subEntries = ENTRY_PATTERN.split(entryStr);
                RDN entry = new RDN();
                for (String subEntryStr : subEntries) {
                    SubEntry subEntry = parseSingleSubEntry(dn, subEntryStr);
                    entry.addSubEntry(subEntry);
                }
                dn.addLast(entry);
            }
        }

        return dn;
    }

    // parse single sub-entry and add it to the "dn" . Assumption is that subentry is something like "uid=bar" and does not contain + character
    private static SubEntry parseSingleSubEntry(LdapMapDn dn, String subEntryStr) {
        String[] rdn = SUB_ENTRY_PATTERN.split(subEntryStr);
        if (rdn.length >1) {
            return new SubEntry(rdn[0].trim(), rdn[1].trim());
        } else {
            return new SubEntry(rdn[0].trim(), "");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LdapMapDn)) {
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

    private static String toString(Collection<RDN> entries) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (RDN rdn : entries) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append(rdn.toString());
        }

        return builder.toString();
    }

    /**
     * @return first entry. Usually entry corresponding to something like "uid=joe" from the DN like "uid=joe,dc=something,dc=org"
     */
    public RDN getFirstRdn() {
        return entries.getFirst();
    }

    private static String unescapeValue(String escaped) {
        // Something needed to handle non-String types?
        return Rdn.unescapeValue(escaped).toString();
    }

    private static String escapeValue(String unescaped) {
        // Something needed to handle non-String types?
        return Rdn.escapeValue(unescaped);
    }

    /**
     *
     * @return DN like "dc=something,dc=org" from the DN like "uid=joe,dc=something,dc=org".
     * Returned DN will be new clone not related to the original DN instance.
     *
     */
    public LdapMapDn getParentDn() {
        LinkedList<RDN> parentDnEntries = new LinkedList<>(entries);
        parentDnEntries.remove();
        return new LdapMapDn(parentDnEntries);
    }

    public boolean isDescendantOf(LdapMapDn expectedParentDn) {
        int parentEntriesCount = expectedParentDn.entries.size();

        Deque<RDN> myEntries = new LinkedList<>(this.entries);
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
        rdnValue = escapeValue(rdnValue);
        entries.addFirst(new RDN(new SubEntry(rdnName, rdnValue)));
    }

    public void addFirst(RDN entry) {
        entries.addFirst(entry);
    }

    private void addLast(RDN entry) {
        entries.addLast(entry);
    }

    /**
     * Single RDN inside the DN. RDN usually consists of single item like "uid=john" . In some rare cases, it can have multiple
     * sub-entries like "uid=john+sn=Doe"
     */
    public static class RDN {

        private final List<SubEntry> subs = new LinkedList<>();

        private RDN() {
        }

        private RDN(SubEntry subEntry) {
            subs.add(subEntry);
        }

        private void addSubEntry(SubEntry subEntry) {
            subs.add(subEntry);
        }

        /**
         * @return Keys in the RDN. Returned list is the copy, which is not linked to the original RDN
         */
        public List<String> getAllKeys() {
            return subs.stream().map(SubEntry::getAttrName).collect(Collectors.toList());
        }

        /**
         * Assume that RDN is something like "uid=john", then this method will return "john" in case that attrName is "uid" .
         * This is useful in case that RDN is multi-key - something like "uid=john+cn=John Doe" and we want to return just "john" as the value of "uid"
         *
         * The returned value will be unescaped
         *
         */
        public String getAttrValue(String attrName) {
            for (SubEntry sub : subs) {
                if (attrName.equalsIgnoreCase(sub.attrName)) {
                    return LdapMapDn.unescapeValue(sub.attrValue);
                }
            }
            return null;
        }

        public void setAttrValue(String attrName, String newAttrValue) {
            for (SubEntry sub : subs) {
                if (attrName.equalsIgnoreCase(sub.attrName)) {
                    sub.attrValue = escapeValue(newAttrValue);
                    return;
                }
            }
            addSubEntry(new SubEntry(attrName, escapeValue(newAttrValue)));
        }

        public boolean removeAttrValue(String attrName) {
            SubEntry toRemove = null;
            for (SubEntry sub : subs) {
                if (attrName.equalsIgnoreCase(sub.attrName)) {
                    toRemove = sub;
                }
            }

            if (toRemove != null) {
                subs.remove(toRemove);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return toString(true);
        }

        /**
         *
         * @param escaped indicates whether return escaped or unescaped values. EG. "uid=john,comma" VS "uid=john\,comma"
         */
        public String toString(boolean escaped) {
            StringBuilder builder = new StringBuilder();

            boolean first = true;
            for (SubEntry subEntry : subs) {
                if (first) {
                    first = false;
                } else {
                    builder.append('+');
                }
                builder.append(subEntry.toString(escaped));
            }

            return builder.toString();
        }
    }

    private static class SubEntry {
        private final String attrName;
        private String attrValue;

        private SubEntry(String attrName, String attrValue) {
            this.attrName = attrName;
            this.attrValue = attrValue;
        }

        private String getAttrName() {
            return attrName;
        }

        @Override
        public String toString() {
            return toString(true);
        }

        private String toString(boolean escaped) {
            String val = escaped ? attrValue : unescapeValue(attrValue);
            return attrName + '=' + val;
        }
    }
}
