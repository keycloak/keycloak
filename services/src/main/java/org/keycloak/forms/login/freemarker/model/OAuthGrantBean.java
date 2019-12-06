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
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.OrderedModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantBean {

    private static OrderedModel.OrderedModelComparator<ClientScopeEntry> COMPARATOR_INSTANCE = new OrderedModel.OrderedModelComparator<>();

    private List<ClientScopeEntry> clientScopesRequested = new ArrayList<>();
    private String code;
    private ClientModel client;

    public OAuthGrantBean(String code, ClientModel client, List<ClientScopeModel> clientScopesRequested) {
        this.code = code;
        this.client = client;

        for (ClientScopeModel clientScope : clientScopesRequested) {
            this.clientScopesRequested.add(new ClientScopeEntry(clientScope.getConsentScreenText(), clientScope.getGuiOrder()));
        }
        this.clientScopesRequested.sort(COMPARATOR_INSTANCE);
    }

    public String getCode() {
        return code;
    }


    public String getClient() {
        return client.getClientId();
    }


    public List<ClientScopeEntry> getClientScopesRequested() {
        return clientScopesRequested;
    }


    // Converting ClientScopeModel due the freemarker limitations. It's not able to read "getConsentScreenText" default method defined on interface
    public static class ClientScopeEntry implements OrderedModel {

        private final String consentScreenText;
        private final String guiOrder;

        private ClientScopeEntry(String consentScreenText, String guiOrder) {
            this.consentScreenText = consentScreenText;
            this.guiOrder = guiOrder;
        }

        public String getConsentScreenText() {
            return consentScreenText;
        }

        @Override
        public String getGuiOrder() {
            return guiOrder;
        }
    }
}
