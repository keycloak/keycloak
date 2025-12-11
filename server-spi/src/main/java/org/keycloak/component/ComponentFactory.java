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
package org.keycloak.component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ComponentFactory<CreatedType, ProviderType extends Provider> extends ProviderFactory<ProviderType>, ConfiguredProvider {
    CreatedType create(KeycloakSession session, ComponentModel model);

    @Override
    default ProviderType create(KeycloakSession session) {
        return null;
    }

    /**
     * Called before a component is created or updated.  Allows you to validate the configuration
     *
     * @param session
     * @param realm
     * @param model
     * @throws ComponentValidationException
     */
    default
    void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException
    {

    }

    /**
     * Called after a component is created
     *
     * @param session
     * @param realm
     * @param model
     */
    default
    void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {

    }


    /**
     * Called after the component is updated.
     *
     * @param session
     * @param realm
     * @param oldModel old saved model
     * @param newModel new configuration
     */
    default
    void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {

    }

    /**
     * Called before the component is removed.
     *
     * @param session
     * @param realm
     * @param model model of the component, which is going to be removed
     */
    default
    void preRemove(KeycloakSession session, RealmModel realm, ComponentModel model) {

    }

    /**
     * These are config properties that are common across all implementation of this component type
     *
     * @return
     */
    default
    List<ProviderConfigProperty> getCommonProviderConfigProperties() {
        return Collections.emptyList();
    }

    /**
     * This is metadata about this component type.  Its really configuration information about the component type and not
     * an individual instance
     *
     * @return
     */
    default
    Map<String, Object> getTypeMetadata() {
        return Collections.emptyMap();

    }


}
