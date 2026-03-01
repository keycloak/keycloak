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

package com.acme.provider.legacy.jpa.entity;

import java.util.Collections;
import java.util.Map;
import org.keycloak.it.TestProvider;

public class CustomJpaEntityProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { Realm.class };
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Collections.singletonMap("persistence.xml", "persistence.xml");
    }
}
