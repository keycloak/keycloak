/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.enums.AuthProtocol;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractGeneratedSecretKeyProvider implements KeyProvider {

    private final KeyStatus status;
    private final ComponentModel model;
    private final String kid;
    private final SecretKey secretKey;
    private final KeyUse use;
    private final List<AuthProtocol> authProtocols;
    private String type;
    private final String algorithm;

    public AbstractGeneratedSecretKeyProvider(ComponentModel model, KeyUse use, String type, String algorithm, List<AuthProtocol> authProtocols) {
        this.status = KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true));
        this.kid = model.get(Attributes.KID_KEY);
        this.model = model;
        this.use = use;
        this.type = type;
        this.algorithm = algorithm;
        this.authProtocols = authProtocols;

        if (model.hasNote(SecretKey.class.getName())) {
            secretKey = model.getNote(SecretKey.class.getName());
        } else {
            secretKey = KeyUtils.loadSecretKey(Base64Url.decode(model.get(Attributes.SECRET_KEY)), JavaAlgorithm.getJavaAlgorithm(algorithm));
            model.setNote(SecretKey.class.getName(), secretKey);
        }
    }

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        KeyWrapper key = new KeyWrapper();

        key.setProviderId(model.getId());
        key.setProviderPriority(model.get("priority", 0l));

        key.setKid(kid);
        key.setUses(Arrays.asList(use));
        key.setType(type);
        key.setAlgorithm(algorithm);
        key.setStatus(status);
        key.setSecretKey(secretKey);
        key.setAuthProtocols(authProtocols);

        return Stream.of(key);
    }

    @Override
    public void close() {
    }

}
