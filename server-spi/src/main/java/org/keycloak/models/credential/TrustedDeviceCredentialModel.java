/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.credential;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.credential.dto.TrustedDeviceCredentialData;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * @author Norbert Kelemen
 * @version $Revision: 1 $
 */
public class TrustedDeviceCredentialModel extends CredentialModel {
    private static final Logger logger = Logger.getLogger(TrustedDeviceCredentialModel.class);

    public static final String TYPE = "trusted-device";
    private static final String SECRET_HASH_ALGORITHM = JavaAlgorithm.SHA512;

    private final TrustedDeviceCredentialData credentialData;

    private TrustedDeviceCredentialModel(TrustedDeviceCredentialData credentialData) {
        this.credentialData = credentialData;
    }

    public static TrustedDeviceCredentialModel create(DeviceRepresentation device, String secret) {
        TrustedDeviceCredentialData credentialData = new TrustedDeviceCredentialData(device, SECRET_HASH_ALGORITHM);
        // Generate a new ID manually to be able to use it for cookie creation and distinguish the credentials
        String id = UUID.randomUUID().toString();
        String userLabel = device.getOs() + ' ' + device.getOsVersion() + " / " + device.getBrowser() + " (" + id.substring(0, 8) + ")";

        var hashedSecret = Base64.getEncoder().encodeToString(hashSecret(secret, SECRET_HASH_ALGORITHM));

        try {
            TrustedDeviceCredentialModel tdCredentialModel = new TrustedDeviceCredentialModel(credentialData);

            tdCredentialModel.setId(id);
            tdCredentialModel.setType(TYPE);
            tdCredentialModel.setUserLabel(userLabel);
            tdCredentialModel.setCreatedDate(Time.currentTimeMillis());
            tdCredentialModel.setSecretData(hashedSecret);
            tdCredentialModel.setCredentialData(JsonSerialization.writeValueAsString(credentialData));

            return tdCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TrustedDeviceCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            TrustedDeviceCredentialData credentialData = isBlank(credentialModel.getCredentialData()) ?
                    null :
                    JsonSerialization.readValue(credentialModel.getCredentialData(), TrustedDeviceCredentialData.class);

            return getTrustedDeviceCredentialModel(credentialModel, credentialData);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustedDeviceCredentialModel getTrustedDeviceCredentialModel(CredentialModel credentialModel, TrustedDeviceCredentialData credentialData) {
        TrustedDeviceCredentialModel tdCredentialModel = new TrustedDeviceCredentialModel(credentialData);
        tdCredentialModel.setId(credentialModel.getId());
        tdCredentialModel.setType(credentialModel.getType());
        tdCredentialModel.setUserLabel(credentialModel.getUserLabel());
        tdCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
        tdCredentialModel.setSecretData(credentialModel.getSecretData());
        tdCredentialModel.setCredentialData(credentialModel.getCredentialData());
        return tdCredentialModel;
    }

    private static byte[] hashSecret(String rawSecret, String algorithm) {
        Objects.requireNonNull(rawSecret, "rawSecret cannot be null");
        return HashUtils.hash(algorithm, rawSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean verifySecret(String rawInputSecret) {
        String savedSecretHash = getSecretData();
        if (savedSecretHash == null || savedSecretHash.isBlank()) {
            logger.warn("No saved Trusted Device secret hash found");
            return false;
        }

        String hashAlgorithm = credentialData.getSecretHashAlgorithm();
        if(hashAlgorithm == null || hashAlgorithm.isBlank()) {
            logger.warn("No saved Trusted Device secret hash algorithm found");
            return false;
        }

        try {
            byte[] hashedInputBackupCode = hashSecret(rawInputSecret, hashAlgorithm);
            byte[] savedCode = Base64.getDecoder().decode(savedSecretHash);
            return MessageDigest.isEqual(hashedInputBackupCode, savedCode);
        } catch (IllegalArgumentException iae) {
            logger.warnf("Error when validating Trusted Device secret", iae);
            return false;
        }
    }
}
