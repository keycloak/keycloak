/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.attributeprocessing;

import org.keycloak.userprofile.Attributes;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:external.Martin.Idel@bosch.io">Martin Idel</a>
 */
public interface UserAttributeProcessorProvider extends Provider {

    /**
     * Function to create a listener. This listener will be called for any changing attribute
     * if the verifyProfile action is executed.
     *
     * @param attributes the attributes on listener creation, intended to access their metadata
     * @return a listener function which gets called on attribute updates
     */
    BiConsumer<String, UserModel> createListener(Attributes attributes);
}
