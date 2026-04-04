/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.updaters;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 * Updater for role scope attributes. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class RoleScopeUpdater extends ServerResourceUpdater<RoleScopeUpdater, RoleScopeResource, List<RoleRepresentation>> {

    public RoleScopeUpdater(RoleScopeResource resource) {
        super(resource, resource::listAll, null);
        this.updater = this::update;
    }

    public RoleScopeUpdater add(RoleRepresentation representation) {
        rep.add(representation);
        return this;
    }

    public RoleScopeUpdater remove(RoleRepresentation representation) {
        rep.remove(representation);
        return this;
    }

    public RoleScopeUpdater removeByName(String name) {
        for (Iterator<RoleRepresentation> it = rep.iterator(); it.hasNext();) {
            RoleRepresentation mapper = it.next();
            if (name.equals(mapper.getName())) {
                it.remove();
                break;
            }
        }
        return this;
    }

    private void update(List<RoleRepresentation> expectedRoles) {
        List<RoleRepresentation> currentRoles = resource.listAll();

        Set<String> currentRoleIds = currentRoles.stream().map(RoleRepresentation::getId).collect(Collectors.toSet());
        Set<String> expectedRoleIds = expectedRoles.stream().map(RoleRepresentation::getId).collect(Collectors.toSet());

        List<RoleRepresentation> toAdd = expectedRoles.stream()
          .filter(role -> ! currentRoleIds.contains(role.getId()))
          .collect(Collectors.toList());
        List<RoleRepresentation> toRemove = currentRoles.stream()
          .filter(role -> ! expectedRoleIds.contains(role.getId()))
          .collect(Collectors.toList());

        resource.add(toAdd);
        resource.remove(toRemove);
    }

}
