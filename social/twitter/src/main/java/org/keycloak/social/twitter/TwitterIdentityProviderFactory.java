/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.social.twitter;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.social.SocialIdentityProviderFactory;

/**
 * @author Pedro Igor
 */
public class TwitterIdentityProviderFactory extends AbstractIdentityProviderFactory<TwitterIdentityProvider> implements SocialIdentityProviderFactory<TwitterIdentityProvider> {

    @Override
    public String getName() {
        return "Twitter";
    }

    @Override
    public TwitterIdentityProvider create(IdentityProviderModel model) {
        return new TwitterIdentityProvider(new OAuth2IdentityProviderConfig(getId(), model.getId(), model.getName(), model.getConfig()));
    }

    @Override
    public String getId() {
        return "twitter";
    }
}
