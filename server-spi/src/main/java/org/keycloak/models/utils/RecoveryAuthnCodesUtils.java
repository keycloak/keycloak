package org.keycloak.models.utils;

import java.util.Optional;
import java.util.function.Supplier;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecoveryAuthnCodesUtils {

    private static final int QUANTITY_OF_CODES_TO_GENERATE = 12;
    private static final int CODE_LENGTH = 12;
    public static final char[] UPPERNUM = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789".toCharArray();
    private static final SecretGenerator SECRET_GENERATOR = SecretGenerator.getInstance();
    public static final String NOM_ALGORITHM_TO_HASH = Algorithm.RS512;
    public static final int NUM_HASH_ITERATIONS = 1;
    public static final String RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE = "recovery-codes-error-invalid";
    public static final String FIELD_RECOVERY_CODE_IN_BROWSER_FLOW = "recoveryCodeInput";

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
        Supplier<String> code = () -> SECRET_GENERATOR.randomString(CODE_LENGTH,UPPERNUM);
        return Stream.generate(code).limit(QUANTITY_OF_CODES_TO_GENERATE).collect(Collectors.toList());
    }

    /**
     * Checks the user storage for the credential. If not found it will look for the credential in the local storage
     *
     * @param user - User model
     * @return - a optional  credential model
     */
    public static Optional<CredentialModel> getCredential(UserModel user) {
        return user.credentialManager()
                .getFederatedCredentialsStream()
                .filter(c -> RecoveryAuthnCodesCredentialModel.TYPE.equals(c.getType()))
                .findFirst()
                .or(() -> user.credentialManager().getStoredCredentialsByTypeStream(RecoveryAuthnCodesCredentialModel.TYPE).findFirst());
    }
}
