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

package org.keycloak.common.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public enum AccountRestApiVersion {
    V1_ALPHA1("v1alpha1");

    public static final AccountRestApiVersion DEFAULT = V1_ALPHA1;
    private static final Map<String,AccountRestApiVersion> ENUM_MAP;

    static {
        Map<String, AccountRestApiVersion> map = new HashMap<>();
        for (AccountRestApiVersion value : AccountRestApiVersion.values()) {
            map.put(value.getStrVersion(), value);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    private final String strVersion;

    AccountRestApiVersion(String strVersion) {
        this.strVersion = strVersion;
    }

    public static AccountRestApiVersion get(String strVersion) {
        return ENUM_MAP.get(strVersion);
    }

    public String getStrVersion() {
        return strVersion;
    }
}
