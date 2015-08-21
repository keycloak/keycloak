package org.keycloak.federation.ldap.idm.model;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPDn {

    private final Deque<Entry> entries = new LinkedList<>();

    public static LDAPDn fromString(String dnString) {
        LDAPDn dn = new LDAPDn();

        String[] rdns = dnString.split("(?<!\\\\),");
        for (String entryStr : rdns) {
            String[] rdn = entryStr.split("(?<!\\\\)=");
            dn.addLast(rdn[0].trim(), rdn[1].trim());
        }

        return dn;
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
        return firstEntry.attrName + "=" + firstEntry.attrValue;
    }

    /**
     * @return string attribute name like "uid" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getFirstRdnAttrName() {
        Entry firstEntry = entries.getFirst();
        return firstEntry.attrName;
    }

    /**
     *
     * @return string like "dc=something,dc=org" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getParentDn() {
        LinkedList<Entry> parentDnEntries = new LinkedList<>(entries);
        parentDnEntries.remove();
        return toString(parentDnEntries);
    }

    public void addFirst(String rdnName, String rdnValue) {
        rdnValue = escape(rdnValue);
        entries.addFirst(new Entry(rdnName, rdnValue));
    }

    private void addLast(String rdnName, String rdnValue) {
        entries.addLast(new Entry(rdnName, rdnValue));
    }

    // Need to escape "john,dot" to be "john\,dot"
    private String escape(String rdnValue) {
        if (rdnValue.contains(",")) {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (String split : rdnValue.split(",")) {
                if (!first) {
                    result.append("\\,");
                } else {
                    first = false;
                }
                result.append(split);
            }
            return result.toString();
        } else {
            return rdnValue;
        }
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
