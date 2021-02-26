/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.role;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public abstract class MapRoleAdapter<K> extends AbstractRoleModel<MapRoleEntity<K>> implements RoleModel {

    private static final Logger LOG = Logger.getLogger(MapRoleAdapter.class);

    public MapRoleAdapter(KeycloakSession session, RealmModel realm, MapRoleEntity<K> entity) {
        super(session, realm, entity);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public boolean isComposite() {
        return ! entity.getCompositeRoles().isEmpty();
    }

    @Override
    public Stream<RoleModel> getCompositesStream() {
        LOG.tracef("%% %s(%s).getCompositesStream():%d - %s", entity.getName(), entity.getId().toString(), entity.getCompositeRoles().size(), getShortStackTrace());
        return entity.getCompositeRoles().stream()
                .map(uuid -> session.roles().getRoleById(realm, uuid))
                .filter(Objects::nonNull);
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        LOG.tracef("%s(%s).addCompositeRole(%s(%s))%s", entity.getName(), entity.getId().toString(), role.getName(), role.getId(), getShortStackTrace());
        entity.addCompositeRole(role.getId());
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        LOG.tracef("%s(%s).removeCompositeRole(%s(%s))%s", entity.getName(), entity.getId().toString(), role.getName(), role.getId(), getShortStackTrace());
        entity.removeCompositeRole(role.getId());
    }

    @Override
    public boolean isClientRole() {
        return entity.isClientRole();
    }

    @Override
    public String getContainerId() {
        return isClientRole() ? entity.getClientId() : entity.getRealmId();
    }

    @Override
    public RoleContainerModel getContainer() {
        return isClientRole() ? session.clients().getClientById(realm, entity.getClientId()) : realm;
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        entity.setAttribute(name, values);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return entity.getAttributes();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return this.equals(role) || KeycloakModelUtils.searchFor(role, this, new HashSet<>());
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return getAttributes().getOrDefault(name, Collections.EMPTY_LIST).stream();
    }

    @Override
    public String toString() {
        return "MapRoleAdapter{" + getId() + '}';
    }

}
