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

package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.List;

/**
 * Factory interface for {@link RequiredActionProvider RequiredActionProvider's}.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionFactory extends ProviderFactory<RequiredActionProvider> {

    /**
     * Display text used in admin console to reference this required action
     *
     * @return
     */
    String getDisplayText();

    /**
     * Flag indicating whether the execution of the required action by the same circumstances
     * (e.g. by one and the same action token) should only be permitted once.
     * @return
     */
    default boolean isOneTimeAction() {
        return false;
    }

    /**
     * Indicates whether this required action can be configured via the admin ui.
     * @return
     */
    default boolean isConfigurable() {
        List<ProviderConfigProperty> configMetadata = getConfigMetadata();
        return configMetadata != null && !configMetadata.isEmpty();
    }

    /**
     * Allows users to validate the provided configuration for this required action. Users can throw a {@link org.keycloak.models.ModelValidationException} to indicate that the configuration is invalid.
     *
     * @param session
     * @param realm
     * @param model
     */
    default void validateConfig(KeycloakSession session, RealmModel realm, RequiredActionConfigModel model) {
    }
}
