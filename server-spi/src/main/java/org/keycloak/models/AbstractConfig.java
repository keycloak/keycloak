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
package org.keycloak.models;

import java.io.Serializable;
import java.util.function.Supplier;

public abstract class AbstractConfig implements Serializable {

    @Deprecated(since = "26.5", forRemoval = true)
    protected transient Supplier<RealmModel> realm;

    // Make sure setters are not called when calling this from constructor to avoid DB updates
    protected transient Supplier<RealmModel> realmForWrite;

    protected void persistRealmAttribute(String name, String value) {
        RealmModel realm = realmForWrite == null ? null : this.realmForWrite.get();
        if (realm != null) {
            realm.setAttribute(name, value);
        }
    }

    protected void persistRealmAttribute(String name, Integer value) {
        RealmModel realm = realmForWrite == null ? null : this.realmForWrite.get();
        if (realm != null) {
            realm.setAttribute(name, value);
        }
    }
}
