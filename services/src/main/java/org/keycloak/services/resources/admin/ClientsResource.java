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
package org.keycloak.services.resources.admin;

import static java.lang.Boolean.TRUE;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.AuthorizationService;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.validation.ClientValidator;
import org.keycloak.services.validation.PairwiseClientValidator;
import org.keycloak.services.validation.ValidationMessages;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base resource class for managing a realm's clients.
 *
 * @resource Clients
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientsResource {
    protected static final Logger logger = Logger.getLogger(ClientsResource.class);
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientsResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);

    }

    /**
     * Get clients belonging to the realm
     *
     * Returns a list of clients belonging to the realm
     *
     * @param clientId filter by clientId
     * @param viewableOnly filter clients that cannot be viewed in full by admin
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ClientRepresentation> getClients(@QueryParam("clientId") String clientId, @QueryParam("viewableOnly") @DefaultValue("false") boolean viewableOnly) {
        List<ClientRepresentation> rep = new ArrayList<>();

        if (clientId == null || clientId.trim().equals("")) {
            List<ClientModel> clientModels = realm.getClients();
            auth.clients().requireList();
            boolean view = auth.clients().canView();
            for (ClientModel clientModel : clientModels) {
                if (view || auth.clients().canView(clientModel)) {
                    ClientRepresentation representation = ModelToRepresentation.toRepresentation(clientModel, session);
                    rep.add(representation);
                    representation.setAccess(auth.clients().getAccess(clientModel));
                } else if (!viewableOnly && auth.clients().canView(clientModel)) {
                    ClientRepresentation client = new ClientRepresentation();
                    client.setId(clientModel.getId());
                    client.setClientId(clientModel.getClientId());
                    client.setDescription(clientModel.getDescription());
                    rep.add(client);
                }
            }
        } else {
            ClientModel clientModel = realm.getClientByClientId(clientId);
            if (clientModel != null) {
                if (auth.clients().canView(clientModel)) {
                    ClientRepresentation representation = ModelToRepresentation.toRepresentation(clientModel, session);
                    representation.setAccess(auth.clients().getAccess(clientModel));
                    rep.add(representation);
                } else if (!viewableOnly && auth.clients().canView(clientModel)){
                    ClientRepresentation client = new ClientRepresentation();
                    client.setId(clientModel.getId());
                    client.setClientId(clientModel.getClientId());
                    client.setDescription(clientModel.getDescription());
                    rep.add(client);

                } else {
                    throw new ForbiddenException();
                }
            }
        }
        return rep;
    }

    private AuthorizationService getAuthorizationService(ClientModel clientModel) {
        return new AuthorizationService(session, clientModel, auth, adminEvent);
    }

    /**
     * Create a new client
     *
     * Client's client_id must be unique!
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClient(final ClientRepresentation rep) {
        auth.clients().requireManage();

        ValidationMessages validationMessages = new ValidationMessages();
        if (!ClientValidator.validate(rep, validationMessages) || !PairwiseClientValidator.validate(session, rep, validationMessages)) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(
                    validationMessages.getStringMessages(),
                    validationMessages.getStringMessages(messages),
                    Response.Status.BAD_REQUEST
            );
        }

        try {
            ClientModel clientModel = ClientManager.createClient(session, realm, rep, true);

            if (TRUE.equals(rep.isServiceAccountsEnabled())) {
                UserModel serviceAccount = session.users().getServiceAccount(clientModel);

                if (serviceAccount == null) {
                    new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
                }
            }

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), clientModel.getId()).representation(rep).success();

            if (TRUE.equals(rep.getAuthorizationServicesEnabled())) {
                AuthorizationService authorizationService = getAuthorizationService(clientModel);

                authorizationService.enable(true);

                ResourceServerRepresentation authorizationSettings = rep.getAuthorizationSettings();

                if (authorizationSettings != null) {
                    authorizationService.resourceServer().importSettings(authorizationSettings);
                }
            }

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }

    /**
     * Base path for managing a specific client.
     *
     * @param id id of client (not client-id)
     * @return
     */
    @Path("{id}")
    public ClientResource getClient(final @PathParam("id") String id) {

        ClientModel clientModel = realm.getClientById(id);
        if (clientModel == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.clients().canList()) throw new NotFoundException("Could not find client");
            else throw new ForbiddenException();
        }

        session.getContext().setClient(clientModel);

        ClientResource clientResource = new ClientResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }


    @Path("{id}/clientRoleResourceByUser/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Set<ResourceRepresentation> getClientRoleResourceByUser(final @PathParam("id") String id, final @PathParam("userId") String userId) {
        auth.clients().requireView();

        final Set<ResourceRepresentation> resourceRepresentations = new LinkedHashSet<>();
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(id);
        ClientModel clientModel = realm.getClientById(id);
        if (clientModel != null && resourceServer != null && auth.clients().canView(clientModel)) {
            UserModel user = session.users().getUserById(userId, realm);
            Set<RoleModel> userRoles = getUserRoles(user, clientModel);
            policyStore.findByType("resource", resourceServer.getId()).forEach(policy -> {
                policyToResource(resourceRepresentations, authorizationProvider, resourceServer, userRoles, policy);
            });
            policyStore.findByType("scope", resourceServer.getId()).forEach(policy -> {
                policyScopeToResource(resourceRepresentations, authorizationProvider, resourceServer, userRoles, policy);
            });
        } else {
            throw new ForbiddenException();
        }

        return resourceRepresentations;
    }


    /**
     * @param resourceRepresentations
     * @param authorizationProvider
     * @param resourceServer
     * @param userRoles
     * @param policy
     */
    private void policyScopeToResource(Set<ResourceRepresentation> resourceRepresentations, AuthorizationProvider authorizationProvider, ResourceServer resourceServer, Set<RoleModel> userRoles, Policy policy) {
        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        Iterator<Policy> associatedPoliciesIterable = associatedPolicies.iterator();
        while (associatedPoliciesIterable.hasNext()) {
            Policy associatedPolicie = associatedPoliciesIterable.next();
            Map<String, String> config = new HashMap(associatedPolicie.getConfig());
            String roles = associatedPolicie.getConfig().get("roles");
            if (roles != null) {
                List<Map<String, Object>> roleConfig;
                try {
                    roleConfig = JsonSerialization.readValue(roles, List.class);
                } catch (Exception e) {
                    throw new RuntimeException("Malformed configuration for role policy [" + policy.getName() + "].", e);
                }
                if (!roleConfig.isEmpty()) {
                    for (Map<String, Object> roleMap : roleConfig) {
                        if (containRole(userRoles, String.valueOf(roleMap.get("id")))) {
                            for (Resource resource : authorizationProvider.getStoreFactory().getResourceStore().findByScope(policy.getScopes().stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId())) {
                                resourceRepresentations.add(ModelToRepresentation.toRepresentation(resource, resourceServer, authorizationProvider));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param resourceRepresentations
     * @param authorizationProvider
     * @param resourceServer
     * @param userRoles
     * @param policy
     */
    private void policyToResource(Set<ResourceRepresentation> resourceRepresentations, AuthorizationProvider authorizationProvider, ResourceServer resourceServer, Set<RoleModel> userRoles, Policy policy) {
        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        Iterator<Policy> associatedPoliciesIterable = associatedPolicies.iterator();
        while (associatedPoliciesIterable.hasNext()) {
            Policy associatedPolicie = associatedPoliciesIterable.next();
            Map<String, String> config = new HashMap(associatedPolicie.getConfig());
            String roles = associatedPolicie.getConfig().get("roles");
            if (roles != null) {
                List<Map<String, Object>> roleConfig;
                try {
                    roleConfig = JsonSerialization.readValue(roles, List.class);
                } catch (Exception e) {
                    throw new RuntimeException("Malformed configuration for role policy [" + policy.getName() + "].", e);
                }
                if (!roleConfig.isEmpty()) {
                    for (Map<String, Object> roleMap : roleConfig) {
                        if (containRole(userRoles, String.valueOf(roleMap.get("id")))) {
                            for (Resource resource : policy.getResources()) {
                                resourceRepresentations.add(ModelToRepresentation.toRepresentation(resource, resourceServer, authorizationProvider));
                            }
                        }
                    }
                }
            }
        }
    }


    @Path("{id}/clientRoleSubResourceByUser/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Set<ResourceRepresentation> getClientRoleSubResourceByUser(final @PathParam("id") String id, final @PathParam("userId") String userId) {
        auth.clients().requireView();

        final Set<ResourceRepresentation> representations = getClientRoleResourceByUser(id, userId);
        final Map<String, Set<ResourceRepresentation>> representationMap = new HashMap<>();
        final Set<ResourceRepresentation> resourceRepresentations = new LinkedHashSet<>();
        Set<ResourceRepresentation> subResourceRepresentations;
        for (ResourceRepresentation resourceRepresentation : representations) {
            String parent = getSingleAttribute("parent", resourceRepresentation);
            if (parent != null) {
                if (representationMap.containsKey(parent)) {
                    representationMap.get(parent).add(resourceRepresentation);
                } else {
                    subResourceRepresentations = new LinkedHashSet<>();
                    subResourceRepresentations.add(resourceRepresentation);
                    representationMap.put(parent, subResourceRepresentations);
                }
            } else {
                resourceRepresentations.add(resourceRepresentation);
            }
        }

        for (ResourceRepresentation resourceRepresentation : representations) {
            String name = resourceRepresentation.getName();
            if (representationMap.containsKey(name)) {
                resourceRepresentation.getSubResources().addAll(representationMap.get(name));
            }
        }

        return resourceRepresentations;
    }


    /**
     * @param name
     * @param resourceRepresentation
     * @return
     */
    public String getSingleAttribute(String name, ResourceRepresentation resourceRepresentation) {
        if (resourceRepresentation.getAttributes() == null) {
            return null;
        }
        List<String> values = resourceRepresentation.getAttributes().getOrDefault(name, Collections.emptyList());
        if (values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }


    /**
     * 获得用户角色
     *
     * @param user
     * @param client
     * @return
     */
    private Set<RoleModel> getUserRoles(UserModel user, ClientModel client) {
        Set<RoleModel> userRoles = new HashSet<>();
        for (RoleModel roleModel : client.getRoles()) {
            if (user.hasRole(roleModel)) {
                userRoles.add(roleModel);
            }
        }
        return userRoles;
    }


    /**
     * 是否包角色
     *
     * @param roleModels
     * @param roleId
     * @return
     */
    private boolean containRole(Set<RoleModel> roleModels, String roleId) {
        boolean bool = false;
        for (RoleModel roleModel : roleModels) {
            if (roleModel.getId().equals(roleId)) {
                bool = true;
                break;
            }
        }
        return bool;
    }

}
