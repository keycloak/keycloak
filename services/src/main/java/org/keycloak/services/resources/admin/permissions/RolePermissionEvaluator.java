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

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RolePermissionEvaluator {
    boolean canList(RoleContainerModel container);

    void requireList(RoleContainerModel container);

    boolean canMapRole(RoleModel role);
    void requireMapRole(RoleModel role);

    boolean canManage(RoleModel role);

    void requireManage(RoleModel role);

    boolean canView(RoleModel role);

    void requireView(RoleModel role);

    boolean canMapClientScope(RoleModel role);
    void requireMapClientScope(RoleModel role);

    boolean canMapComposite(RoleModel role);
    void requireMapComposite(RoleModel role);

    boolean canManage(RoleContainerModel container);

    void requireManage(RoleContainerModel container);

    boolean canView(RoleContainerModel container);

    void requireView(RoleContainerModel container);

}
