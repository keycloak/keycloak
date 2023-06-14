/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.credential;

import com.webauthn4j.converter.util.ObjectConverter;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnPasswordlessCredentialProviderFactory implements CredentialProviderFactory<WebAuthnPasswordlessCredentialProvider>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "keycloak-webauthn-passwordless";

    private ObjectConverter converter;

    @Override
    public CredentialProvider create(KeycloakSession session) {
        return new WebAuthnPasswordlessCredentialProvider(session, createOrGetObjectConverter());
    }

    private ObjectConverter createOrGetObjectConverter() {
        if (converter == null) {
            synchronized (this) {
                if (converter == null) {
                    converter = new ObjectConverter();
                }
            }
        }
        return converter;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
    }
}
