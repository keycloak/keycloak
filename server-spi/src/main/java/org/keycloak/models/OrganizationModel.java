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

package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface OrganizationModel {

    String USER_ORGANIZATION_ATTRIBUTE = "kc.org";

    String getId();

    void setName(String name);

    String getName();

    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    String getFirstAttribute(String name);

    Stream<String> getAttributeStream(String name);

    Map<String, List<String>> getAttributes();
}
