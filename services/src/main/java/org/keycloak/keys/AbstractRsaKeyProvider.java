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

import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.*;
import org.keycloak.models.RealmModel;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractRsaKeyProvider implements KeyProvider {

    private final KeyStatus status;

    private final ComponentModel model;

    private final KeyWrapper key;

    private final String algorithm;

    public AbstractRsaKeyProvider(RealmModel realm, ComponentModel model) {
        this.model = model;
        this.status = KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true));
        this.algorithm = model.get(Attributes.ALGORITHM_KEY, Algorithm.RS256);

        if (model.hasNote(KeyWrapper.class.getName())) {
            key = model.getNote(KeyWrapper.class.getName());
        } else {
            key = loadKey(realm, model);
            model.setNote(KeyWrapper.class.getName(), key);
        }
    }

    protected abstract KeyWrapper loadKey(RealmModel realm, ComponentModel model);

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return Stream.of(key);
    }

    protected KeyWrapper createKeyWrapper(KeyPair keyPair, X509Certificate certificate, KeyUse keyUse) {
        return createKeyWrapper(keyPair, certificate, Collections.emptyList(), keyUse);
    }

    protected KeyWrapper createKeyWrapper(KeyPair keyPair, X509Certificate certificate, List<X509Certificate> certificateChain,
        KeyUse keyUse) {
        KeyWrapper key = new KeyWrapper();

        key.setProviderId(model.getId());
        key.setProviderPriority(model.get("priority", 0l));

        key.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(keyUse == null ? KeyUse.SIG : keyUse);
        key.setType(KeyType.RSA);
        key.setAlgorithm(algorithm);
        key.setStatus(status);
        key.setPrivateKey(keyPair.getPrivate());
        key.setPublicKey(keyPair.getPublic());
        key.setCertificate(certificate);

        if (!certificateChain.isEmpty()) {
            if (certificate != null && !certificate.equals(certificateChain.get(0))) {
                // just in case the chain does not contain the end-user certificate
                certificateChain.add(0, certificate);
            }
            key.setCertificateChain(certificateChain);
        }

        return key;
    }

}
