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
 *
 */

package org.keycloak.client.clienttype;

import java.util.Set;

import org.keycloak.models.ClientModel;

/**
 * TODO:client-types javadocs
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientType {

    String getName();

    // Augment client type at runtime
    // Can be property name (like "standardFlow" or "rootUrl") or attributeName (like "pkceEnabled")
    boolean isApplicable(String optionName);

    // Return the value of particular option (if it can be provided by clientType) or return null if this option is not provided by client type
    <T> T getTypeValue(String optionName, Class<T> optionType);

    Set<String> getOptionNames();

    // Augment at the client type
    ClientModel augment(ClientModel client);
}
