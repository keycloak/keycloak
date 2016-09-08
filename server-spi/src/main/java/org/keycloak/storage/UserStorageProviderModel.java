/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Stored configuration of a User Storage provider instance.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class UserStorageProviderModel extends ComponentModel {

    public static Comparator<UserStorageProviderModel> comparator = new Comparator<UserStorageProviderModel>() {
        @Override
        public int compare(UserStorageProviderModel o1, UserStorageProviderModel o2) {
            return o1.getPriority() - o2.getPriority();
        }
    };

    public UserStorageProviderModel() {
        setProviderType(UserStorageProvider.class.getName());
    }

    public UserStorageProviderModel(ComponentModel copy) {
        super(copy);
    }

    public int getPriority() {
        String priority = getConfig().getFirst("priority");
        if (priority == null) return 0;
        return Integer.valueOf(priority);

    }

    public void setPriority(int priority) {
        getConfig().putSingle("priority", Integer.toString(priority));
    }
}
