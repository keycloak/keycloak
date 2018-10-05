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

package org.keycloak.protocol;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ProtocolMapper extends Provider, ProviderFactory<ProtocolMapper>,ConfiguredProvider {
    String getProtocol();
    String getDisplayCategory();
    String getDisplayType();

    /**
     * Priority of this protocolMapper implementation. Lower goes first.
     * @return
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Called when instance of mapperModel is created/updated for this protocolMapper through admin endpoint
     *
     * @param session
     * @param realm
     * @param client client or clientTemplate
     * @param mapperModel
     * @throws ProtocolMapperConfigException if configuration provided in mapperModel is not valid
     */
    default void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
    };

}
