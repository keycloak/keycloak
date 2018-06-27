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
package org.keycloak.social.verimi;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

/**
 * @author keycloak@rlfnb.de
 */
public class VerimiIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public VerimiIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public void setKeystoreLocation(String keystoreLocation) {
        getConfig().put("keystoreLocation", keystoreLocation);
    }

    public String getKeystoreLocation() {
        return getConfig().get("keystoreLocation");
    }

    public String getData() {
        return getConfig().get("data");
    }

    public void setData(String data) {
        getConfig().put("data", data);
    }

    public boolean isReadBasket() {
        String readbasket = getConfig().get("readbasket");
        return readbasket == null ? false : Boolean.valueOf(readbasket);
    }

    public void setReadBasket(boolean readBasket) {
        getConfig().put("readbasket", String.valueOf(readBasket));
    }

    @Override
    public String getDefaultScope() {
        if (isReadBasket()) {
            return getConfig().get("defaultScope") + " read_basket";
        } else {
            return getConfig().get("defaultScope");
        }
    }

}
