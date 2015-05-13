package org.keycloak.federation.ldap.idm.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPDn {

    private final List<Entry> entries = new LinkedList<Entry>();

    public static LDAPDn fromString(String dnString) {
        LDAPDn dn = new LDAPDn();

        String[] rdns = dnString.split(",");
        for (String entryStr : rdns) {
            String[] rdn = entryStr.split("=");
            dn.addToBottom(rdn[0].trim(), rdn[1].trim());
        }

        return dn;
    }

    @Override
    public String toString() {
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
        Entry firstEntry = entries.get(0);
        return firstEntry.attrName + "=" + firstEntry.attrValue;
    }

    /**
     * @return string attribute name like "uid" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getFirstRdnAttrName() {
        Entry firstEntry = entries.get(0);
        return firstEntry.attrName;
    }

    /**
     *
     * @return string like "dc=something,dc=org" from the DN like "uid=joe,dc=something,dc=org"
     */
    public String getParentDn() {
        StringBuilder builder = new StringBuilder();

        int n = 0;
        for (Entry rdn : entries) {
            n++;
            if (n > 2) {
                builder.append(",");
            }
            if (n >= 2) {
                builder.append(rdn.attrName).append("=").append(rdn.attrValue);
            }
        }

        return builder.toString();
    }

    public void addToHead(String rdnName, String rdnValue) {
        entries.add(0, new Entry(rdnName, rdnValue));
    }

    public void addToBottom(String rdnName, String rdnValue) {
        entries.add(new Entry(rdnName, rdnValue));
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
