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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecoveryAuthnCodesUtils {

    public static final int QUANTITY_OF_RECOVERY_AUTHN_CODES_TO_GENERATE = 15;
    private static final RandomString RECOVERY_AUTHN_CODES_RANDOM_STRING = new RandomString(4,
                                                                                            new SecureRandom(),
                                                                                        RandomString.upper+RandomString.digits);
    public static final boolean SHOULD_SAVE_RAW_RECOVERY_AUTHN_CODE = false;

    public static final String NOM_ALGORITHM_TO_HASH = Algorithm.RS512;
    public static final int NUM_HASH_ITERATIONS = 1;

    public static final String NAM_TEMPLATE_LOGIN_INPUT_RECOVERY_AUTHN_CODE  = "login-recovery-authn-code-input.ftl";
    public static final String NAM_TEMPLATE_LOGIN_CONFIG_RECOVERY_AUTHN_CODE = "login-recovery-authn-code-config.ftl";
    public static final String RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE = "recovery-codes-error-invalid";
    public static final String FIELD_RECOVERY_CODE_IN_BROWSER_FLOW = "recoveryCodeInput";
    public static final String FIELD_GENERATED_RECOVERY_AUTHN_CODES_HIDDEN = "generatedRecoveryAuthnCodes";
    public static final String FIELD_GENERATED_AT_HIDDEN = "generatedAt";
    public static final String FIELD_USER_LABEL_HIDDEN = "userLabel";
    public static final String FIELD_RECOVERY_CODE_IN_DIRECT_GRANT_FLOW = "recovery_code";

    public static String hashRawCode(String rawGeneratedCode) {
        String hashedCode = null;

        if (StringUtil.isNotBlank(rawGeneratedCode)) {

            byte[] rawCodeHashedAsBytes = HashUtils.hash(JavaAlgorithm.getJavaAlgorithmForHash(NOM_ALGORITHM_TO_HASH),
                                                         rawGeneratedCode.getBytes(StandardCharsets.UTF_8));

            if (rawCodeHashedAsBytes != null && rawCodeHashedAsBytes.length > 0) {
                hashedCode = Base64.encodeBytes(rawCodeHashedAsBytes);
            }
        }

        return hashedCode;
    }

    public static boolean verifyRecoveryCodeInput(String rawInputRecoveryCode, String hashedSavedRecoveryCode) {

        String hashedInputBackupCode = hashRawCode(rawInputRecoveryCode);

        return (hashedInputBackupCode.equals(hashedSavedRecoveryCode));
    }

    public static List<String> generateRawCodes() {
        return Stream.generate(RecoveryAuthnCodesUtils::newCode)
                     .limit(QUANTITY_OF_RECOVERY_AUTHN_CODES_TO_GENERATE)
                     .collect(Collectors.toList());
    }

    private static String newCode() {
        return String.join("-",
                           RECOVERY_AUTHN_CODES_RANDOM_STRING.nextString(),
                           RECOVERY_AUTHN_CODES_RANDOM_STRING.nextString(),
                           RECOVERY_AUTHN_CODES_RANDOM_STRING.nextString());
    }

}
