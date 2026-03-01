/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa.support;

import java.lang.reflect.Proxy;
import java.util.HashSet;

import jakarta.persistence.EntityManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityManagerProxyTest {

    @Test
    public void testClosure() {
        HashSet<EntityManagerProxy> proxies = new HashSet<EntityManagerProxy>();
        EntityManager em = (EntityManager)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{EntityManager.class}, (proxy, method, args) -> null);
        EntityManager proxy = EntityManagerProxy.create(em, proxies, false, 1);
        assertEquals(1, proxies.size());
        proxy.close();
        // once closed, the entity manager should not be tracked
        assertTrue(proxies.isEmpty());
    }

}
