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
package org.keycloak.representations.idm;

import java.util.Map;

public class UserProfileAttributeGroupMetadata {

    private String name;
    private String displayHeader;
    private String displayDescription;
    private Map<String, Object> annotations;

    public UserProfileAttributeGroupMetadata() {
    }

    public UserProfileAttributeGroupMetadata(String name, String displayHeader, String displayDescription, Map<String, Object> annotations) {
        this.name = name;
        this.displayHeader = displayHeader;
        this.displayDescription = displayDescription;
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public String getDisplayHeader() {
        return displayHeader;
    }


    public String getDisplayDescription() {
        return displayDescription;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }
}