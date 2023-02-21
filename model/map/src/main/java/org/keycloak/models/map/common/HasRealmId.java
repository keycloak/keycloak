/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.common;

/**
 * Interface for all objects which are bound to a realm and retain reference to its ID.
 * @author hmlnarik
 */
public interface HasRealmId {

    /**
     * Returns realm ID of the entity.
     * @return See description
     */
    String getRealmId();

    /**
     * Sets the realm ID of this object.
     * @param realmId Realm ID.
     */
    void setRealmId(String realmId);
}
