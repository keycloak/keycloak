/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Implementation of {@link CredentialOfferStorage} that uses AWS S3 for storage.
 * Supports IAM Roles automatically via the default AWS credentials provider chain.
 */
public class S3CredentialOfferStorage implements CredentialOfferStorage {

    private final S3Client s3Client;
    private final String bucketName;

    public S3CredentialOfferStorage(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void putOfferState(KeycloakSession session, CredentialOfferState entry) {
        String key = entry.getNonce();
        String json = JsonSerialization.valueAsString(entry);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build(), RequestBody.fromString(json));
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to put offer state to S3", e);
        }
    }

    @Override
    public CredentialOfferState findOfferStateByNonce(KeycloakSession session, String nonce) {
        return getObject(nonce);
    }

    @Override
    public CredentialOfferState findOfferStateByCode(KeycloakSession session, String code) {
        // In the default implementation, codes and nonces are both used as keys.
        // For S3, we use the same pattern.
        return getObject(code);
    }

    @Override
    public CredentialOfferState findOfferStateByCredentialId(KeycloakSession session, String credId) {
        return getObject(credId);
    }

    private CredentialOfferState getObject(String key) {
        try {
            InputStream is = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build(), ResponseTransformer.toInputStream());
            return JsonSerialization.readValue(is, CredentialOfferState.class);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            throw new RuntimeException("Failed to get offer state from S3", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize offer state from S3", e);
        }
    }

    @Override
    public void replaceOfferState(KeycloakSession session, CredentialOfferState entry) {
        putOfferState(session, entry);
    }

    @Override
    public void removeOfferState(KeycloakSession session, CredentialOfferState entry) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(entry.getNonce())
                    .build());
            entry.getPreAuthorizedCode().ifPresent(code -> {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(code)
                        .build());
            });
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to remove offer state from S3", e);
        }
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
