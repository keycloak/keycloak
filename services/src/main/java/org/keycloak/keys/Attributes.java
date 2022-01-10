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

package org.keycloak.keys;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.provider.ProviderConfigProperty;

import static org.keycloak.provider.ProviderConfigProperty.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Attributes {

    String PRIORITY_KEY = "priority";
    ProviderConfigProperty PRIORITY_PROPERTY = new ProviderConfigProperty(PRIORITY_KEY, "Priority", "Priority for the provider", STRING_TYPE, "0");

    String ENABLED_KEY = "enabled";
    ProviderConfigProperty ENABLED_PROPERTY = new ProviderConfigProperty(ENABLED_KEY, "Enabled", "Set if the keys are enabled", BOOLEAN_TYPE, "true");

    String ACTIVE_KEY = "active";
    ProviderConfigProperty ACTIVE_PROPERTY = new ProviderConfigProperty(ACTIVE_KEY, "Active", "Set if the keys can be used for signing", BOOLEAN_TYPE, "true");

    String PRIVATE_KEY_KEY = "privateKey";
    ProviderConfigProperty PRIVATE_KEY_PROPERTY = new ProviderConfigProperty(PRIVATE_KEY_KEY, "Private RSA Key", "Private RSA Key encoded in PEM format", FILE_TYPE, null, true);

    String CERTIFICATE_KEY = "certificate";
    ProviderConfigProperty CERTIFICATE_PROPERTY = new ProviderConfigProperty(CERTIFICATE_KEY, "X509 Certificate", "X509 Certificate encoded in PEM format", FILE_TYPE, null);

    String KEY_SIZE_KEY = "keySize";
    ProviderConfigProperty KEY_SIZE_PROPERTY = new ProviderConfigProperty(KEY_SIZE_KEY, "Key size", "Size for the generated keys", LIST_TYPE, "2048", "1024", "2048", "4096");

    String KEY_USE = "keyUse";
    ProviderConfigProperty KEY_USE_PROPERTY = new ProviderConfigProperty(KEY_USE, "Key use", "Whether the key should be used for signing or encryption.", LIST_TYPE,
            KeyUse.SIG.getSpecName(), KeyUse.SIG.getSpecName(), KeyUse.ENC.getSpecName());

    String KID_KEY = "kid";

    String SECRET_KEY = "secret";

    String SECRET_SIZE_KEY = "secretSize";
    ProviderConfigProperty SECRET_SIZE_PROPERTY = new ProviderConfigProperty(SECRET_SIZE_KEY, "Secret size", "Size in bytes for the generated secret", LIST_TYPE,
            String.valueOf(GeneratedHmacKeyProviderFactory.DEFAULT_HMAC_KEY_SIZE),
            "16", "24", "32", "64", "128", "256", "512");

    String ALGORITHM_KEY = "algorithm";

    ProviderConfigProperty RS_ALGORITHM_PROPERTY = new ProviderConfigProperty(ALGORITHM_KEY, "Algorithm", "Intended algorithm for the key", LIST_TYPE,
            Algorithm.RS256,
            Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.PS256, Algorithm.PS384, Algorithm.PS512);

    ProviderConfigProperty HS_ALGORITHM_PROPERTY = new ProviderConfigProperty(ALGORITHM_KEY, "Algorithm", "Intended algorithm for the key", LIST_TYPE,
            Algorithm.HS256,
            Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);

    ProviderConfigProperty RS_ENC_ALGORITHM_PROPERTY = new ProviderConfigProperty(ALGORITHM_KEY, "Algorithm", "Intended algorithm for the key encryption", LIST_TYPE,
            JWEConstants.RSA_OAEP,
            JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);

}
