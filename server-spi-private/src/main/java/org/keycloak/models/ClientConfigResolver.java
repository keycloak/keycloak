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
 * TODO: remove this class entirely?
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientConfigResolver {
    protected ClientModel client;

    public ClientConfigResolver(ClientModel client) {
        this.client = client;
    }

    public String resolveAttribute(String name) {
        return client.getAttribute(name);
    }

    public boolean isFrontchannelLogout() {
        return client.isFrontchannelLogout();
    }

    boolean isConsentRequired() {
        return client.isConsentRequired();
    }

    boolean isStandardFlowEnabled() {
        return client.isStandardFlowEnabled();
    }

    boolean isServiceAccountsEnabled() {
        return client.isServiceAccountsEnabled();
    }
}
