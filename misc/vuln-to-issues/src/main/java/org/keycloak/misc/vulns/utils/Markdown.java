package org.keycloak.misc.vulns.utils;

public class Markdown {

    public static String toCveLink(String cveId) {
        if (cveId.startsWith("CVE-")) {
            return "[" + cveId + "](https://www.cve.org/CVERecord?id=" + cveId + ")";
        } else if (cveId.startsWith("SNYK-")) {
            return "[" + cveId + "](https://security.snyk.io/vuln/" + cveId + ")";
        } else {
            return cveId;
        }
    }

}
