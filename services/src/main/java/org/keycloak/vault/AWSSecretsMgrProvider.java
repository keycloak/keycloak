/**
 * Copyright 2021 OutSystems and/or its affiliates and other
 * contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.vault;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class AWSSecretsMgrProvider extends AbstractVaultProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    protected AWSSecretsManager client;

    /**
     * Creates an instance of {@code AbstractVaultProvider} with the specified realm and list of key resolvers.
     *
     * @param realm the name of the keycloak realm.
     * @param resolvers a {@link List} containing the configured key resolvers.
     * @param region a {@link String} containing the AWS region, e.g. us-west-2.
     */
    public AWSSecretsMgrProvider(String realm, List<VaultKeyResolver> resolvers, String region) {
        super(realm, resolvers);

        client = AWSSecretsManagerClientBuilder.standard()
                .withRegion(region)
                .build();
    }

    @Override
    protected VaultRawSecret obtainSecretInternal(String vaultKey) {
        final GetSecretValueResult secretValue;
        try {
            secretValue = client.getSecretValue(new GetSecretValueRequest().withSecretId(vaultKey));
        } catch (Exception e) {
            logger.error("Failed to retrieve secret " + vaultKey, e);
            throw e;
        }

        return new VaultRawSecret() {
            @Override
            public Optional<ByteBuffer> get() {
                if (secretValue.getSecretString() != null) {
                    return Optional.of(ByteBuffer.wrap(secretValue.getSecretString().getBytes(StandardCharsets.UTF_8)));
                } else if (secretValue.getSecretBinary() != null) {
                    return Optional.of(Base64.getDecoder().decode(secretValue.getSecretBinary()));
                }
                return Optional.empty();
            }

            @Override
            public Optional<byte[]> getAsArray() {
                if (secretValue.getSecretString() != null) {
                    return Optional.of(secretValue.getSecretString().getBytes(StandardCharsets.UTF_8));
                } else if (secretValue.getSecretBinary() != null) {
                    return Optional.of(Base64.getDecoder().decode(secretValue.getSecretBinary()).array());
                }
                return Optional.empty();
            }

            @Override
            public void close() {
                // Overwrite secret with garbage
                byte[] array = new byte[128];
                new SecureRandom().nextBytes(array);
                secretValue.setSecretString(new String(array, StandardCharsets.UTF_8));
            }
        };
    }

    @Override
    public void close() {
        client.shutdown();
    }
}
