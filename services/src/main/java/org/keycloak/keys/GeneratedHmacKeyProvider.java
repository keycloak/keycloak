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
import org.keycloak.jose.jws.AlgorithmType;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GeneratedHmacKeyProvider implements HmacKeyProvider {

    private final boolean enabled;

    private final boolean active;

    private final ComponentModel model;
    private final String kid;
    private final SecretKey secretKey;

    public GeneratedHmacKeyProvider(ComponentModel model) {
        this.enabled = model.get(Attributes.ENABLED_KEY, true);
        this.active = model.get(Attributes.ACTIVE_KEY, true);
        this.kid = model.get(Attributes.KID_KEY);
        this.model = model;

        if (model.hasNote(SecretKey.class.getName())) {
            secretKey = model.getNote(SecretKey.class.getName());
        } else {
            secretKey = KeyUtils.loadSecretKey(model.get(Attributes.SECRET_KEY));
            model.setNote(SecretKey.class.getName(), secretKey);
        }
    }

    @Override
    public SecretKey getSecretKey() {
        return isActive() ? secretKey : null;
    }

    @Override
    public SecretKey getSecretKey(String kid) {
        return isEnabled() && kid.equals(this.kid) ? secretKey : null;
    }

    @Override
    public String getKid() {
        return isActive() ? kid : null;
    }

    @Override
    public List<HmacKeyMetadata> getKeyMetadata() {
        if (kid != null && secretKey != null) {
            HmacKeyMetadata k = new HmacKeyMetadata();
            k.setProviderId(model.getId());
            k.setProviderPriority(model.get(Attributes.PRIORITY_KEY, 0l));
            k.setKid(kid);
            if (isActive()) {
                k.setStatus(KeyMetadata.Status.ACTIVE);
            } else if (isEnabled()) {
                k.setStatus(KeyMetadata.Status.PASSIVE);
            } else {
                k.setStatus(KeyMetadata.Status.DISABLED);
            }
            return Collections.singletonList(k);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
    }

    private boolean isEnabled() {
        return secretKey != null && enabled;
    }

    private boolean isActive() {
        return isEnabled() && active;
    }

}
