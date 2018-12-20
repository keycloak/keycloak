/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.social.vkontakte;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

/**
 * @author Wladislaw Mitzel <mitzel@tawadi.de>
 */
public class VKontakteIdentityProviderFactory extends AbstractIdentityProviderFactory<VKontakteIdentityProvider>
        implements SocialIdentityProviderFactory<VKontakteIdentityProvider> {

    public static final String PROVIDER_ID = "vkontakte";
    private static final String PROVIDER_NAME = "VKontakte";

    public String getName() {
        return PROVIDER_NAME;
    }

    public VKontakteIdentityProvider create(KeycloakSession keycloakSession, IdentityProviderModel identityProviderModel) {
        return new VKontakteIdentityProvider(keycloakSession, new VKontakteIdentityProviderConfig(identityProviderModel));
    }

    public String getId() {
        return PROVIDER_ID;
    }
}
