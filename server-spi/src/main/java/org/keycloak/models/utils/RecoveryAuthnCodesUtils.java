package org.keycloak.models.utils;

import org.keycloak.common.util.Base64;
import org.keycloak.common.util.RandomString;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecoveryAuthnCodesUtils {

    private static final int QUANTITY_OF_CODES_TO_GENERATE = 15;
    private static final RandomString RANDOM_GENERATOR = new RandomString(12, new SecureRandom(),
            RandomString.upper + RandomString.digits);
    public static final String NOM_ALGORITHM_TO_HASH = Algorithm.RS512;
    public static final int NUM_HASH_ITERATIONS = 1;
    public static final String RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE = "recovery-codes-error-invalid";
    public static final String FIELD_RECOVERY_CODE_IN_BROWSER_FLOW = "recoveryCodeInput";
    public static final String FIELD_RECOVERY_CODE_IN_DIRECT_GRANT_FLOW = "recovery_code";

    public static String hashRawCode(String rawGeneratedCode) {
        Objects.requireNonNull(rawGeneratedCode, "rawGeneratedCode cannot be null");

        byte[] rawCodeHashedAsBytes = HashUtils.hash(JavaAlgorithm.getJavaAlgorithmForHash(NOM_ALGORITHM_TO_HASH),
                rawGeneratedCode.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeBytes(rawCodeHashedAsBytes);
    }

    public static boolean verifyRecoveryCodeInput(String rawInputRecoveryCode, String hashedSavedRecoveryCode) {

        String hashedInputBackupCode = hashRawCode(rawInputRecoveryCode);

        return (hashedInputBackupCode.equals(hashedSavedRecoveryCode));
    }

    public static List<String> generateRawCodes() {
        return Stream.generate(RANDOM_GENERATOR::nextString)
                .limit(QUANTITY_OF_CODES_TO_GENERATE)
                .collect(Collectors.toList());
    }

}
