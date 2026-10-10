package org.keycloak.misc.vulns;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class CveIgnoreParser {

    private Map<String, CveIgnore> ignoreMap = new HashMap<>();

    public void removeIgnored(List<Vuln> vulns) {
        ignoreByIgnoreFile(vulns);
        ignoreKeycloakPackages(vulns);
    }

    private void ignoreByIgnoreFile(List<Vuln> vulns) {
        ListIterator<Vuln> itr = vulns.listIterator();
        while (itr.hasNext()) {
            Vuln vuln = itr.next();
            Set<String> affectedStreams = vuln.affectedStreams();
            Set<String> ignoredStreams = new HashSet<>();
            for (String s : affectedStreams) {
                CveIgnore cveIgnore = getCveIgnore(s);
                if (cveIgnore != null) {
                    if (cveIgnore.ignoreLevel() != null && cveIgnore.ignoreLevel().compareTo(vuln.severity()) <= 0) {
                        ignoredStreams.add(s);
                    } else if (cveIgnore.vulnerabilities() != null && cveIgnore.vulnerabilities().stream().anyMatch(i -> vuln.cveId().equals(i.cveId()) && vuln.packageName().equals(i.packageName()))) {
                        ignoredStreams.add(s);
                    }
                }
            }

            if (!ignoredStreams.isEmpty()) {
                if (ignoredStreams.equals(affectedStreams)) {
                    System.out.println("* Ignored: cveId: " + vuln.cveId() + ", packageName: " + vuln.packageName());
                    itr.remove();
                } else {
                    vuln.affectedStreams().removeAll(ignoredStreams);
                    System.out.println("* Partially ignored: cveId: " + vuln.cveId() + ", packageName: " + vuln.packageName() + ", ignoredStreams: " + String.join(", ", ignoredStreams) + ", remainingAffectedStreams: " + String.join(", ", vuln.affectedStreams()));
                }
            }
        }
    }

    private void ignoreKeycloakPackages(List<Vuln> vulns) {
        ListIterator<Vuln> itr = vulns.listIterator();
        Set<String> ignoredKeycloakCves = new HashSet<>();
        while (itr.hasNext()) {
            Vuln vuln = itr.next();
            if (vuln.packageName().startsWith("org.keycloak")) {
                itr.remove();
                ignoredKeycloakCves.add(vuln.cveId());
            }
        }
        if (!ignoredKeycloakCves.isEmpty()) {
            System.out.println("* Ignored " + ignoredKeycloakCves.size() + " CVEs in Keycloak (" + String.join(", ", ignoredKeycloakCves) + ")");
        }
    }

    private CveIgnore getCveIgnore(String stream) {
        return ignoreMap.computeIfAbsent(stream, s -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/" + Config.REPOSITORY + "/refs/heads/" + stream + "/.cveignore.yaml");
                try (InputStream is = url.openStream()) {
                    return YamlUtil.read(is, CveIgnore.class);
                } catch (FileNotFoundException e) {
                    return new CveIgnore(null, Collections.emptyList());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
