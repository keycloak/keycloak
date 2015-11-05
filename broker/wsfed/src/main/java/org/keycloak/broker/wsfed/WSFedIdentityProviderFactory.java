/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.broker.wsfed;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class WSFedIdentityProviderFactory extends AbstractIdentityProviderFactory<WSFedIdentityProvider> {

    public static final String PROVIDER_ID = "wsfed";

    @Override
    public String getName() {
        return "WS-Fed";
    }

    @Override
    public WSFedIdentityProvider create(IdentityProviderModel model) {
        return new WSFedIdentityProvider(new WSFedIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(InputStream inputStream) {
        //TODO: Implement parsing of metadata
        return new HashMap<String, String>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
