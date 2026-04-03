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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * Factory for creating {@link S3CredentialOfferStorage} providers.
 */
public class S3CredentialOfferStorageFactory implements CredentialOfferStorageFactory {

    public static final String PROVIDER_ID = "s3";
    private String bucketName;
    private String region;
    private String endpoint;

    @Override
    public CredentialOfferStorage create(KeycloakSession session) {
        S3ClientBuilder builder = S3Client.builder();
        if (region != null) {
            builder.region(Region.of(region));
        }
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return new S3CredentialOfferStorage(builder.build(), bucketName);
    }

    @Override
    public void init(Config.Scope config) {
        this.bucketName = config.get("bucketName", System.getenv("S3_BUCKET_NAME"));
        this.region = config.get("region", System.getenv("S3_REGION"));
        this.endpoint = config.get("endpoint", System.getenv("S3_ENDPOINT"));

        if (this.bucketName == null) {
            // Log warning or throw error if bucket name is missing
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
