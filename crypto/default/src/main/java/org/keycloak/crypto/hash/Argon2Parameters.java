package org.keycloak.crypto.hash;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Argon2Parameters {

    public static String DEFAULT_TYPE = "id";
    public static String DEFAULT_VERSION = "1.3";
    public static int DEFAULT_HASH_LENGTH = 32;
    public static int DEFAULT_MEMORY = 7168;
    public static int DEFAULT_ITERATIONS = 5;
    public static int DEFAULT_PARALLELISM = 1;

    private static Map<String, Integer> types = new LinkedHashMap<>();

    static {
        types.put("id", org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id);
        types.put("d", org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_d);
        types.put("i", org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_i);
    }

    private static Map<String, Integer> versions = new LinkedHashMap<>();

    static {
        versions.put("1.3", org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_VERSION_13);
        versions.put("1.0", org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_VERSION_10);
    }

    public static Set<String> listTypes() {
        return types.keySet();
    }

    public static Set<String> listVersions() {
        return versions.keySet();
    }

    public static int getTypeValue(String type) {
        return types.get(type);
    }

    public static int getVersionValue(String version) {
        return versions.get(version);
    }


}
