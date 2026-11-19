package org.keycloak.misc.vulns.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.misc.vulns.Config;

public class FindDependency {

    private final static Pattern DECLARED_IN = Pattern.compile(".*Version:.*Declared in: (.*), line.*");

    public static Set<String> findDependency(String packageName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(lookupFindDependencyScript(), packageName);
        Process process = pb.start();
        int exit = process.waitFor();
        Set<String> declaredIn = new HashSet<>();
        if (exit == 0) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                for (String l = br.readLine(); l != null; l = br.readLine()) {
                    Matcher m = DECLARED_IN.matcher(l);
                    if (m.matches()) {
                        declaredIn.add(m.group(1));
                    }
                }
            }
        }
        return declaredIn;
    }

    public static String lookupFindDependencyScript() {
        return Config.VULN_SCRIPT_HOME.toPath().resolve("../scripts/find-dependency.sh").normalize().toAbsolutePath().toString();
    }

}
