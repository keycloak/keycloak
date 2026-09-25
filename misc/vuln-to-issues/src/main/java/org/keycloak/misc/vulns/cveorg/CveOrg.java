package org.keycloak.misc.vulns.cveorg;

import org.keycloak.misc.vulns.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class CveOrg {

    public static CveInfo query(String cveId) {
        try (InputStream inputStream = new URL("https://cveawg.mitre.org/api/cve/" + cveId).openStream()) {
            return JsonUtil.read(inputStream, CveInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
