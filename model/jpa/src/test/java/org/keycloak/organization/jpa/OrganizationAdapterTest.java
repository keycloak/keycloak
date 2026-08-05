/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.jpa;

import java.lang.reflect.Proxy;

import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.OrganizationEntity;

import org.junit.Assert;
import org.junit.Test;

public class OrganizationAdapterTest {

    @Test
    public void shouldExposeOwningRealm() {
        RealmModel realm = (RealmModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> { throw new UnsupportedOperationException(); });
        OrganizationAdapter organization = new OrganizationAdapter(null, realm, new OrganizationEntity(), null);

        Assert.assertSame(realm, organization.getRealm());
    }
}
