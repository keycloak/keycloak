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
package org.keycloak.services.resources.admin.permissions;

import org.keycloak.models.GroupModel;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface GroupPermissionEvaluator {
    boolean canList();

    void requireList();

    boolean canManage(GroupModel group);

    void requireManage(GroupModel group);

    boolean canView(GroupModel group);

    void requireView(GroupModel group);

    boolean canManage();

    void requireManage();

    boolean canView();

    void requireView();

    boolean getGroupsWithViewPermission(GroupModel group);

    void requireViewMembers(GroupModel group);

    boolean canManageMembers(GroupModel group);

    boolean canManageMembership(GroupModel group);

    void requireManageMembership(GroupModel group);

    Map<String, Boolean> getAccess(GroupModel group);

    Set<String> getGroupsWithViewPermission();
}
