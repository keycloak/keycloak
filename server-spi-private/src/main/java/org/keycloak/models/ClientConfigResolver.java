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

package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientConfigResolver {
    protected ClientModel client;
    protected ClientTemplateModel clientTemplate;

    public ClientConfigResolver(ClientModel client) {
        this.client = client;
        this.clientTemplate = client.getClientTemplate();
    }

    public String resolveAttribute(String name) {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.getAttribute(name);
        } else {
            return client.getAttribute(name);
        }
    }

    public boolean isFrontchannelLogout() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isFrontchannelLogout();
        }

        return client.isFrontchannelLogout();
    }

    boolean isConsentRequired() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isConsentRequired();
        }

        return client.isConsentRequired();
    }

    boolean isStandardFlowEnabled() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isStandardFlowEnabled();
        }

        return client.isStandardFlowEnabled();
    }

    boolean isServiceAccountsEnabled() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isServiceAccountsEnabled();
        }

        return client.isServiceAccountsEnabled();
    }
}
