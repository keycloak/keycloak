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

package org.keycloak.services.roles;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.StringUtil;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * Role management
 * <p>
 * Every operation enforces its own {@link AdminPermissionEvaluator} checks.
 * The service does not fire admin events, has no dependency on JAX-RS request state or representations;
 * callers are responsible for events and for converting to and from representations.
 * Failures other than authorization are reported as {@link ServiceException} with a suggested HTTP status.
 * <p>
 * Operations taking a {@link RoleContainerModel} and a role name additionally check the container permissions.
 * Operations taking a {@link RoleModel} only check the permissions on the role itself.
 */
public class RoleService {

    private static final String ROLE_NOT_FOUND = "Could not find role";

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public RoleService(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    public Stream<RoleModel> list(RoleContainerModel container, String search, Integer first, Integer max) {
        auth.roles().requireList(container);

        if (search != null && !search.trim().isEmpty()) {
            return container.searchForRolesStream(search, first, max);
        } else if (first != null && max != null) {
            return container.getRolesStream(first, max);
        }
        return container.getRolesStream();
    }

    public RoleModel get(RoleContainerModel container, String name) {
        auth.roles().requireView(container);
        return find(container, name);
    }

    public RoleModel get(RoleModel role) {
        auth.roles().requireView(role);
        return role;
    }

    public RoleModel create(RoleContainerModel container, String name, String description, Map<String, List<String>> attributes) {
        return create(container, name, description, attributes, null, null);
    }

    /**
     * @param realmComposites names of realm roles to add as composites, may be {@code null}
     * @param clientComposites client roles to add as composites keyed by clientId, may be {@code null}; unknown clients are skipped
     */
    public RoleModel create(RoleContainerModel container, String name, String description, Map<String, List<String>> attributes,
            Collection<String> realmComposites, Map<String, List<String>> clientComposites) {
        auth.roles().requireManage(container);

        if (name == null) {
            throw new ServiceException("role has no name", Response.Status.BAD_REQUEST);
        }

        RoleModel role;
        try {
            role = container.addRole(name);
        } catch (ModelDuplicateException e) {
            throw alreadyExists(name);
        }
        role.setDescription(description);

        if (attributes != null) {
            for (Map.Entry<String, List<String>> attr : attributes.entrySet()) {
                role.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        // Handling of nested composite roles for KEYCLOAK-12754
        if (realmComposites != null && !realmComposites.isEmpty()) {
            Set<RoleModel> realmRoles = new LinkedHashSet<>();
            for (String roleName : realmComposites) {
                RoleModel realmRole = realm.getRole(roleName);
                if (realmRole == null) {
                    throw new ServiceException("Realm Role with name " + roleName + " does not exist", Response.Status.NOT_FOUND);
                }
                realmRoles.add(realmRole);
            }
            realmRoles.stream().peek(auth.roles()::requireMapComposite).forEach(role::addCompositeRole);
        }

        if (clientComposites != null && !clientComposites.isEmpty()) {
            for (Map.Entry<String, List<String>> clientIdWithClientRoleNames : clientComposites.entrySet()) {
                ClientModel client = realm.getClientByClientId(clientIdWithClientRoleNames.getKey());
                if (client == null) {
                    continue;
                }
                Set<RoleModel> clientRoles = new LinkedHashSet<>();
                for (String roleName : clientIdWithClientRoleNames.getValue()) {
                    RoleModel clientRole = client.getRole(roleName);
                    if (clientRole == null) {
                        throw new ServiceException("Client Role with name " + roleName + " does not exist", Response.Status.NOT_FOUND);
                    }
                    clientRoles.add(clientRole);
                }
                clientRoles.stream().peek(auth.roles()::requireMapComposite).forEach(role::addCompositeRole);
            }
        }

        return role;
    }

    public RoleModel update(RoleContainerModel container, String name, String newName, String description,
            Map<String, List<String>> attributes) {
        auth.roles().requireManage(container);
        return update(find(container, name), newName, description, attributes);
    }

    /**
     * @param attributes the complete desired attribute set; {@code null} leaves the attributes untouched
     */
    public RoleModel update(RoleModel role, String newName, String description, Map<String, List<String>> attributes) {
        auth.roles().requireManage(role);

        if (isBlank(newName)) {
            throw new ServiceException("role has no name", Response.Status.BAD_REQUEST);
        }

        try {
            String previousName = role.getName();
            if (!Objects.equals(previousName, newName)) {
                role.setName(newName);
                publishNameChange(role, previousName, newName);
            }

            role.setDescription(description);

            if (attributes != null) {
                Set<String> attrsToRemove = new HashSet<>(role.getAttributes().keySet());
                attrsToRemove.removeAll(attributes.keySet());

                for (Map.Entry<String, List<String>> attr : attributes.entrySet()) {
                    role.setAttribute(attr.getKey(), attr.getValue());
                }

                for (String attr : attrsToRemove) {
                    role.removeAttribute(attr);
                }
            }
        } catch (ModelDuplicateException e) {
            throw alreadyExists(newName);
        }

        return role;
    }

    /**
     * @return the removed role; its id, name and container type remain readable for event purposes
     */
    public RoleModel delete(RoleContainerModel container, String name) {
        auth.roles().requireManage(container);
        return delete(find(container, name));
    }

    /**
     * @return the removed role; its id, name and container type remain readable for event purposes
     */
    public RoleModel delete(RoleModel role) {
        RoleModel defaultRole = realm.getDefaultRole();
        if (defaultRole != null && defaultRole.getId().equals(role.getId())) {
            throw new ServiceException(defaultRole.getName() + " is default role of the realm and cannot be removed.",
                    Response.Status.BAD_REQUEST);
        }
        auth.roles().requireManage(role);

        if (!role.getContainer().removeRole(role)) {
            throw new ServiceException("Role not found", Response.Status.NOT_FOUND);
        }
        return role;
    }

    /**
     * @param compositeIds ids of the roles to add as composites
     */
    public RoleModel addComposites(RoleContainerModel container, String name, Collection<String> compositeIds) {
        auth.roles().requireManage(container);
        return addComposites(find(container, name), compositeIds);
    }

    public RoleModel addComposites(RoleModel role, Collection<String> compositeIds) {
        auth.roles().requireManage(role);
        for (String id : compositeIds) {
            RoleModel composite = findComposite(id);
            auth.roles().requireMapComposite(composite);
            role.addCompositeRole(composite);
        }
        return role;
    }

    /**
     * @param compositeIds ids of the composite roles to remove
     */
    public RoleModel removeComposites(RoleContainerModel container, String name, Collection<String> compositeIds) {
        auth.roles().requireManage(container);
        return removeComposites(find(container, name), compositeIds);
    }

    public RoleModel removeComposites(RoleModel role, Collection<String> compositeIds) {
        auth.roles().requireManage(role);
        for (String id : compositeIds) {
            RoleModel composite = findComposite(id);
            auth.roles().requireMapComposite(composite);
            role.removeCompositeRole(composite);
        }
        return role;
    }

    public Stream<RoleModel> getComposites(RoleContainerModel container, String name) {
        auth.roles().requireView(container);
        return viewableComposites(find(container, name), null, null, null);
    }

    public Stream<RoleModel> getComposites(RoleModel role, String search, Integer first, Integer max) {
        auth.roles().requireView(role);
        return viewableComposites(role, search, first, max);
    }

    public Stream<RoleModel> getRealmComposites(RoleContainerModel container, String name) {
        auth.roles().requireView(container);
        return realmComposites(find(container, name));
    }

    public Stream<RoleModel> getRealmComposites(RoleModel role) {
        auth.roles().requireView(role);
        return realmComposites(role);
    }

    public Stream<RoleModel> getClientComposites(RoleContainerModel container, String name, String clientUuid) {
        auth.roles().requireView(container);
        RoleModel role = find(container, name);
        return clientComposites(role, findClient(clientUuid));
    }

    public Stream<RoleModel> getClientComposites(RoleModel role, String clientUuid) {
        auth.roles().requireView(role);
        return clientComposites(role, findClient(clientUuid));
    }

    private Stream<RoleModel> viewableComposites(RoleModel role, String search, Integer first, Integer max) {
        boolean skip = first != null && first > 0;
        boolean limit = max != null && max >= 0;
        // the search overload always queries the database, but is the only one with a defined (name) order.
        // The unordered stream, which can be served from the cache, is only usable when not searching or paginating.
        Stream<RoleModel> composites = (!StringUtil.isNullOrEmpty(search) && !skip && !limit
                ? role.getCompositesStream()
                : role.getCompositesStream(search, null, null))
                .filter(auth.roles()::canView);
        if (skip) {
            composites = composites.skip(first);
        }
        if (limit) {
            composites = composites.limit(max);
        }
        return composites;
    }

    private Stream<RoleModel> realmComposites(RoleModel role) {
        return role.getCompositesStream()
                .filter(composite -> composite.getContainer() instanceof RealmModel)
                .filter(auth.roles()::canView);
    }

    private Stream<RoleModel> clientComposites(RoleModel role, ClientModel client) {
        return role.getCompositesStream()
                .filter(composite -> Objects.equals(composite.getContainer(), client))
                .filter(auth.roles()::canView);
    }

    private RoleModel find(RoleContainerModel container, String name) {
        RoleModel role = container.getRole(name);
        if (role == null) {
            throw new ServiceException(ROLE_NOT_FOUND, Response.Status.NOT_FOUND);
        }
        return role;
    }

    private RoleModel findComposite(String id) {
        RoleModel composite = id == null ? null : realm.getRoleById(id);
        if (composite == null) {
            throw new ServiceException("Could not find composite role", Response.Status.NOT_FOUND);
        }
        return composite;
    }

    private ClientModel findClient(String clientUuid) {
        ClientModel client = realm.getClientById(clientUuid);
        if (client == null) {
            throw new ServiceException("Could not find client", Response.Status.NOT_FOUND);
        }
        return client;
    }

    private static ServiceException alreadyExists(String name) {
        return new ServiceException("Role with name " + name + " already exists", Response.Status.CONFLICT);
    }

    private void publishNameChange(RoleModel role, String previousName, String newName) {
        session.getKeycloakSessionFactory().publish(new RoleModel.RoleNameChangeEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public String getNewName() {
                return newName;
            }

            @Override
            public String getPreviousName() {
                return previousName;
            }

            @Override
            public String getClientId() {
                if (!role.isClientRole()) {
                    return null;
                }

                return ((ClientModel) role.getContainer()).getClientId();
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }
}
