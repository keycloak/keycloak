package org.keycloak.models.sessions.infinispan.untrustedlogin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FingerprintUtil {

    private FingerprintUtil() {
    }

    /**
     * Device fingerprint = SHA-256 of the normalized User-Agent string.
     *
     * NOTE: User-Agent alone is a weak fingerprint (shared across many users, changes on
     * browser auto-update). This is intentionally the v1 signal per the scoped-down design
     * (see issue discussion) — an optional persistent device cookie can be layered in later
     * without changing the storage shape, since we'd just feed a stronger input into this hash.
     */
    public static String hashDevice(String userAgent) {
        String normalized = userAgent == null ? "unknown" : userAgent.trim().toLowerCase();
        return sha256(normalized);
    }

    /**
     * Generalizes an IP address to a /24 (IPv4) or /48 (IPv6) prefix so that ISP/carrier
     * IP rotation within the same network doesn't constantly look "new". This is a coarse
     * heuristic, not a geo-boundary — GeoIpProvider handles actual location resolution
     * for the notification content.
     */
    public static String toSubnet(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        if (ipAddress.contains(":")) {
            // IPv6: keep first 3 hextets (~ /48)
            String[] parts = ipAddress.split(":");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, parts.length); i++) {
                sb.append(parts[i]).append(":");
            }
            return sb.append("0::/48").toString();
        }
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) {
            return ipAddress;
        }
        return octets[0] + "." + octets[1] + "." + octets[2] + ".0/24";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}