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

import com.google.common.collect.ImmutableMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.util.ResolveRelative;

import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientBean {

    protected static final String[] ATTRIBUTES_ALLOWED = { "logoUrl", "policyUri", "tosUri" };

    private KeycloakSession session;
    protected ClientModel client;
    protected Map<String,String> attributes;

    public ClientBean(KeycloakSession session, ClientModel client) {
        this.session = session;
        this.client = client;
        ImmutableMap.Builder<String,String> builder = new ImmutableMap.Builder<>();
        if (client.getAttributes() != null) {
          for (String key : ATTRIBUTES_ALLOWED) {
            String val = client.getAttributes().get(key);
            if (val != null) {
              builder.put(key, val);
            }
          }
        }
        this.attributes = builder.build();
    }

    public String getClientId() {
        return client.getClientId();
    }

    public String getName() {
        return client.getName();
    }

    public String getDescription() {
        return client.getDescription();
    }

    public String getBaseUrl() {
        return ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), client.getBaseUrl());
    }

    public Map<String,String> getAttributes(){
        return attributes;
    }

    public String getAttribute(String key){
        return attributes.get(key);
    }
}
