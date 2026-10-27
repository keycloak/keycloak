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

package org.keycloak.models.utils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;

import org.junit.Assert;
import org.junit.Test;

public class ReadOnlyUserModelDelegateTest {

    @Test
    public void testLeaveGroupDelegates() {
        AtomicBoolean leftGroup = new AtomicBoolean(false);
        UserModel dummyUser = (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class<?>[]{UserModel.class},
                (proxy, method, args) -> {
                    if ("leaveGroup".equals(method.getName())) {
                        leftGroup.set(true);
                        return null;
                    }
                    if ("getUsername".equals(method.getName())) {
                        return "test-user";
                    }
                    return null;
                });

        GroupModel dummyGroup = (GroupModel) Proxy.newProxyInstance(
                GroupModel.class.getClassLoader(),
                new Class<?>[]{GroupModel.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "test-group";
                    }
                    return null;
                });

        ReadOnlyUserModelDelegate readOnlyUser = new ReadOnlyUserModelDelegate(dummyUser);
        readOnlyUser.leaveGroup(dummyGroup);
        Assert.assertTrue("leaveGroup should delegate to underlying user", leftGroup.get());
    }

    @Test(expected = ReadOnlyException.class)
    public void testJoinGroupThrows() {
        UserModel dummyUser = (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class<?>[]{UserModel.class},
                (proxy, method, args) -> {
                    if ("getUsername".equals(method.getName())) {
                        return "test-user";
                    }
                    return null;
                });

        GroupModel dummyGroup = (GroupModel) Proxy.newProxyInstance(
                GroupModel.class.getClassLoader(),
                new Class<?>[]{GroupModel.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "test-group";
                    }
                    return null;
                });

        ReadOnlyUserModelDelegate readOnlyUser = new ReadOnlyUserModelDelegate(dummyUser);
        readOnlyUser.joinGroup(dummyGroup);
    }
}
