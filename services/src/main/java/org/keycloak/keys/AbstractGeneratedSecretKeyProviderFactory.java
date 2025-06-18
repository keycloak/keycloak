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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ConfigurationValidationHelper;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractGeneratedSecretKeyProviderFactory<T extends KeyProvider> implements KeyProviderFactory<T> {

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper validation = SecretKeyProviderUtils.validateConfiguration(model);
        validation.checkList(Attributes.SECRET_SIZE_PROPERTY, false);

        int size = model.get(Attributes.SECRET_SIZE_KEY, getDefaultKeySize());

        if (!(model.contains(Attributes.SECRET_KEY))) {
            generateSecret(model, size);
            logger().debugv("Generated secret for {0}", realm.getName());
        } else {
            int currentSize = Base64Url.decode(model.get(Attributes.SECRET_KEY)).length;
            if (currentSize != size) {
                generateSecret(model, size);
                logger().debugv("Secret size changed, generating new secret for {0}", realm.getName());
            }
        }
    }

    private void generateSecret(ComponentModel model, int size) {
        try {
            byte[] secret = SecretGenerator.getInstance().randomBytes(size);
            model.put(Attributes.SECRET_KEY, Base64Url.encode(secret));

            String kid = KeycloakModelUtils.generateId();
            model.put(Attributes.KID_KEY, kid);
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate secret", t);
        }
    }

    protected abstract Logger logger();

    protected abstract int getDefaultKeySize();
}
