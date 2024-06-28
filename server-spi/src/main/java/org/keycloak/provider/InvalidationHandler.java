/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.provider;

import org.keycloak.models.KeycloakSession;

/**
 * Handles invalidation requests. This interface is specifically implemented by
 * providers that implement a cache of objects that might change in the outside.
 * <p>
 * Note that implementors are expected to react to invalidation requests:
 * invalidate the objects in the cache. They should <b>not</b> initiate
 * invalidation of the same objects neither locally nor via network - that
 * could result in an infinite loop.
 *
 * @author hmlnarik
 */
public interface InvalidationHandler {

    /**
     * Tagging interface for the kinds of invalidatable object
     */
    public interface InvalidableObjectType {}

    public enum ObjectType implements InvalidableObjectType {
        _ALL_, REALM, CLIENT, CLIENT_SCOPE, USER, ROLE, GROUP, COMPONENT, PROVIDER_FACTORY
    }

    /**
     * Invalidates intermediate states of the given objects
     * @param session KeycloakSession
     * @param type Type of the objects to invalidate
     * @param params Parameters used for the invalidation
     */
    void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params);

}
