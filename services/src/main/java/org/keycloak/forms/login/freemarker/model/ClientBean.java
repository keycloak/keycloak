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

package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.ClientModel;
import org.keycloak.services.util.ResolveRelative;

import java.net.URI;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientBean {

    protected ClientModel client;

    private URI requestUri;

    public ClientBean(ClientModel client, URI requestUri) {
        this.client = client;
        this.requestUri = requestUri;
    }

    public String getClientId() {
        return client.getClientId();
    }

    public String getName() {
        return client.getName();
    }

    public String getBaseUrl() {
        return ResolveRelative.resolveRelativeUri(requestUri, client.getRootUrl(), client.getBaseUrl());
    }

    public Map<String,String> getAttributes(){
        return client.getAttributes();
    }

    public String getAttribute(String key){
        return client.getAttribute(key);
    }
}
