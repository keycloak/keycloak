package org.keycloak.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

class ComputedKey {

    private ComputedKey() {
    }

    public static String computeKey(String realm, String type, String alternativeKey) {
        MessageDigest md = getMessageDigest();
        md.update(realm.getBytes(StandardCharsets.UTF_8));
        md.update(type.getBytes(StandardCharsets.UTF_8));
        md.update(alternativeKey.getBytes(StandardCharsets.UTF_8));
        return new String(md.digest(), StandardCharsets.UTF_8);
    }

    public static String computeKey(String realm, String type, Map<String, String> attributes) {
        MessageDigest md = getMessageDigest();
        md.update(realm.getBytes(StandardCharsets.UTF_8));
        md.update(type.getBytes(StandardCharsets.UTF_8));
        attributes.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            md.update(e.getKey().getBytes(StandardCharsets.UTF_8));
            md.update(e.getValue().getBytes(StandardCharsets.UTF_8));
        });
        return new String(md.digest(), StandardCharsets.UTF_8);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
