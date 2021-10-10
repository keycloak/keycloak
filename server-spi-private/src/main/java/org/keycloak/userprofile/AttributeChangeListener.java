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
package org.keycloak.userprofile;

import java.util.List;

import org.keycloak.models.UserModel;

/**
 * Interface of the user profile attribute change listener.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 * @see UserProfile#update(boolean, AttributeChangeListener...)
 * @see UserProfile#update(AttributeChangeListener...)
 */
@FunctionalInterface
public interface AttributeChangeListener {

    /**
     * Method called for each user attribute change.
     * 
     * @param name of the changed user attribute
     * @param user model where new attribute value is applied already (can be null if attribute is removed)
     * @param oldValue of the attribute before the change (can be null)
     */
    void onChange(String name, UserModel user, List<String> oldValue);

}
