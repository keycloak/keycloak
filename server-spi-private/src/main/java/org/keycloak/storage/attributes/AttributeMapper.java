/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Attribute mapper provider for the {@link AttributeFederationProvider}. These mappers translate source attributes from the
 * attribute store to attributes that will be set on the user object.
 */
public interface AttributeMapper extends Provider {
    /**
     * Transform the source attributes to the destination attributes
     * @param session The keycloak session the mapper is being run in
     * @param source The attributes received from the attribute store
     * @param dest The attributes that will be set on the user object
     */
    void transform(KeycloakSession session, Map<String, Object> source, Map<String, String> dest);
}
