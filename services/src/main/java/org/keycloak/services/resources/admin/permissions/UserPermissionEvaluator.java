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

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;

import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserPermissionEvaluator {
    void requireManage();
    void requireManage(UserModel user);
    boolean canManage();
    boolean canManage(UserModel user);

    void requireQuery();
    boolean canQuery();

    void requireView();
    void requireView(UserModel user);
    boolean canView();
    boolean canView(UserModel user);

    void requireImpersonate(UserModel user);
    boolean canImpersonate();
    boolean canImpersonate(UserModel user, ClientModel requester);
    boolean isImpersonatable(UserModel user, ClientModel requester);

    Map<String, Boolean> getAccess(UserModel user);

    void requireMapRoles(UserModel user);
    boolean canMapRoles(UserModel user);

    void requireManageGroupMembership(UserModel user);
    boolean canManageGroupMembership(UserModel user);
    void grantIfNoPermission(boolean grantIfNoPermission);
}
