package org.keycloak.crypto.hash;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Salt;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;
import org.keycloak.tracing.TracingProviderUtil;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.jboss.logging.Logger;

import static org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory.MEMORY_KEY;
import static org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory.PARALLELISM_KEY;
import static org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory.TYPE_KEY;
import static org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory.VERSION_KEY;

public class Argon2PasswordHashProvider implements PasswordHashProvider {

    private static final Logger logger = Logger.getLogger(Argon2PasswordHashProvider.class);
    private final String version;
    private final String type;
    private final int hashLength;
    private final int memory;
    private final int iterations;
    private final int parallelism;
    private final Semaphore cpuCoreSemaphore;

    public Argon2PasswordHashProvider(String version, String type, int hashLength, int memory, int iterations, int parallelism, Semaphore cpuCoreSemaphore) {
        this.version = version;
        this.type = type;
        this.hashLength = hashLength;
        this.memory = memory;
        this.iterations = iterations;
        this.parallelism = parallelism;
        this.cpuCoreSemaphore = cpuCoreSemaphore;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        PasswordCredentialData data = credential.getPasswordCredentialData();

        return iterations == data.getHashIterations() &&
                checkCredData(TYPE_KEY, type, data) &&
                checkCredData(VERSION_KEY, version, data) &&
                checkCredData(Argon2PasswordHashProviderFactory.HASH_LENGTH_KEY, hashLength, data) &&
                checkCredData(MEMORY_KEY, memory, data) &&
                checkCredData(PARALLELISM_KEY, parallelism, data);
    }

    /**
     * Password hashing iterations from password policy is intentionally ignored for now for two reasons. 1) default
     * iterations are 210K, which is way too large for Argon2, and 2) it makes little sense to configure iterations only
     * for Argon2, which should be combined with configuring memory, which is not currently configurable in password
     * policy.
     */
    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        if (iterations == -1) {
            iterations = this.iterations;
        } else if (iterations > 100) {
            logger.warn("Iterations for Argon should be less than 100, using default");
            iterations = this.iterations;
        }

        byte[] salt = Salt.generateSalt();
        String encoded = encode(rawPassword, salt, version, type, hashLength, parallelism, memory, iterations);

        Map<String, List<String>> additionalParameters = new HashMap<>();
        additionalParameters.put(VERSION_KEY, Collections.singletonList(version));
        additionalParameters.put(TYPE_KEY, Collections.singletonList(type));
        additionalParameters.put(Argon2PasswordHashProviderFactory.HASH_LENGTH_KEY, Collections.singletonList(Integer.toString(hashLength)));
        additionalParameters.put(MEMORY_KEY, Collections.singletonList(Integer.toString(memory)));
        additionalParameters.put(PARALLELISM_KEY, Collections.singletonList(Integer.toString(parallelism)));

        return PasswordCredentialModel.createFromValues(Argon2PasswordHashProviderFactory.ID, salt, iterations, additionalParameters, encoded);
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        PasswordCredentialData data = credential.getPasswordCredentialData();
        MultivaluedHashMap<String, String> parameters = data.getAdditionalParameters();
        PasswordSecretData secretData = credential.getPasswordSecretData();

        String version = parameters.getFirst(VERSION_KEY);
        String type = parameters.getFirst(TYPE_KEY);
        int hashLength = Integer.parseInt(parameters.getFirst(Argon2PasswordHashProviderFactory.HASH_LENGTH_KEY));
        int parallelism = Integer.parseInt(parameters.getFirst(PARALLELISM_KEY));
        int memory = Integer.parseInt(parameters.getFirst(MEMORY_KEY));
        int iterations = data.getHashIterations();

        String encoded = encode(rawPassword, secretData.getSalt(), version, type, hashLength, parallelism, memory, iterations);
        return encoded.equals(secretData.getValue());
    }

    @Override
    public String credentialHashingStrength(PasswordCredentialModel credential) {
        MultivaluedHashMap<String, String> parameters = credential.getPasswordCredentialData().getAdditionalParameters();
        return String.format("Argon2%s-%s[m=%s,t=%d,p=%s]", parameters.getFirst(TYPE_KEY), parameters.getFirst(VERSION_KEY), parameters.getFirst(MEMORY_KEY), credential.getPasswordCredentialData().getHashIterations(), parameters.getFirst(PARALLELISM_KEY));
    }

    private String encode(String rawPassword, byte[] salt, String version, String type, int hashLength, int parallelism, int memory, int iterations) {
        var tracing = TracingProviderUtil.getTracingProvider();
        try {
            return tracing.trace(Argon2PasswordHashProvider.class, "encode", span -> {
                try {
                    cpuCoreSemaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

                org.bouncycastle.crypto.params.Argon2Parameters parameters = new org.bouncycastle.crypto.params.Argon2Parameters.Builder(Argon2Parameters.getTypeValue(type))
                        .withVersion(Argon2Parameters.getVersionValue(version))
                        .withSalt(salt)
                        .withParallelism(parallelism)
                        .withMemoryAsKB(memory)
                        .withIterations(iterations).build();

                Argon2BytesGenerator generator = new Argon2BytesGenerator();
                generator.init(parameters);

                byte[] result = new byte[hashLength];
                generator.generateBytes(rawPassword.toCharArray(), result);
                return Base64.getEncoder().encodeToString(result);
            });
        } finally {
            cpuCoreSemaphore.release();
        }
    }

    private boolean checkCredData(String key, int expectedValue, PasswordCredentialData data) {
        String s = data.getAdditionalParameters().getFirst(key);
        Integer v = s != null ? Integer.parseInt(s) : null;
        return v != null && expectedValue == v;
    }

    private boolean checkCredData(String key, String expectedValue, PasswordCredentialData data) {
        String s = data.getAdditionalParameters().getFirst(key);
        return expectedValue.equals(s);
    }

    @Override
    public void close() {
    }
}
