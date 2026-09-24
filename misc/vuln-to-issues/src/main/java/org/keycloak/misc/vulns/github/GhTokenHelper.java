package org.keycloak.misc.vulns.github;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class GhTokenHelper {

    private static final String GH_TOKEN = System.getenv("GH_TOKEN");

    static String getToken() {
        if (GH_TOKEN != null) {
            return GH_TOKEN;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("gh", "auth", "token", "-h", "github.com");
            Process process = pb.start();
            String token = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (token.startsWith("gh")) {
                return token;
            }
        } catch (IOException e) {
        }
        throw new RuntimeException("Failed to retrieve github token");
    }


}
