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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.keycloak.common.ClientConnection;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.user.SynchronizationResult;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * @resource User Storage Provider
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageProviderResource {
    private static final Logger logger = Logger.getLogger(UserStorageProviderResource.class);

    protected RealmModel realm;

    protected AdminPermissionEvaluator auth;

    protected AdminEventBuilder adminEvent;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public UserStorageProviderResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent;
    }

    /**
     * Need this for admin console to display simple name of provider when displaying user detail
     *
     * KEYCLOAK-4328
     *
     * @param id
     * @return
     */
    @GET
    @Path("{id}/name")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getSimpleName(@PathParam("id") String id) {
        auth.users().requireQuery();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("found, but not a UserStorageProvider");
        }

        Map<String, String> data = new HashMap<>();
        data.put("id", model.getId());
        data.put("name", model.getName());
        return data;
    }


    /**
     * Trigger sync of users
     *
     * Action can be "triggerFullSync" or "triggerChangedUsersSync"
     *
     * @param id
     * @param action
     * @return
     */
    @POST
    @Path("{id}/sync")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public SynchronizationResult syncUsers(@PathParam("id") String id,
                                           @QueryParam("action") String action) {
        auth.users().requireManage();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("found, but not a UserStorageProvider");
        }

        UserStorageProviderModel providerModel = new UserStorageProviderModel(model);



        logger.debug("Syncing users");

        UserStorageSyncManager syncManager = new UserStorageSyncManager();
        SynchronizationResult syncResult;
        if ("triggerFullSync".equals(action)) {
            syncResult = syncManager.syncAllUsers(session.getKeycloakSessionFactory(), realm.getId(), providerModel);
        } else if ("triggerChangedUsersSync".equals(action)) {
            syncResult = syncManager.syncChangedUsers(session.getKeycloakSessionFactory(), realm.getId(), providerModel);
        } else if (action == null || action == "") {
            logger.debug("Missing action");
            throw new BadRequestException("Missing action");
        } else {
            logger.debug("Unknown action: " + action);
            throw new BadRequestException("Unknown action: " + action);
        }

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", action);
        eventRep.put("result", syncResult);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(eventRep).success();

        return syncResult;
    }

    /**
     * Remove imported users
     *
     *
     * @param id
     * @return
     */
    @POST
    @Path("{id}/remove-imported-users")
    @NoCache
    public void removeImportedUsers(@PathParam("id") String id) {
        auth.users().requireManage();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("found, but not a UserStorageProvider");
        }

        session.users().removeImportedUsers(realm, id);
    }
    /**
     * Unlink imported users from a storage provider
     *
     *
     * @param id
     * @return
     */
    @POST
    @Path("{id}/unlink-users")
    @NoCache
    public void unlinkUsers(@PathParam("id") String id) {
        auth.users().requireManage();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("found, but not a UserStorageProvider");
        }

        session.users().unlinkUsers(realm, id);
    }

    /**
     * Trigger sync of mapper data related to ldap mapper (roles, groups, ...)
     *
     * direction is "fedToKeycloak" or "keycloakToFed"
     *
     * @return
     */
    @POST
    @Path("{parentId}/mappers/{id}/sync")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public SynchronizationResult syncMapperData(@PathParam("parentId") String parentId, @PathParam("id") String mapperId, @QueryParam("direction") String direction) {
        auth.users().requireManage();

        ComponentModel parentModel = realm.getComponent(parentId);
        if (parentModel == null) throw new NotFoundException("Parent model not found");
        ComponentModel mapperModel = realm.getComponent(mapperId);
        if (mapperModel == null) throw new NotFoundException("Mapper model not found");

        LDAPStorageProvider ldapProvider = (LDAPStorageProvider) session.getProvider(UserStorageProvider.class, parentModel);
        LDAPStorageMapper mapper = session.getProvider(LDAPStorageMapper.class, mapperModel);

        ServicesLogger.LOGGER.syncingDataForMapper(mapperModel.getName(), mapperModel.getProviderId(), direction);

        SynchronizationResult syncResult;
        if ("fedToKeycloak".equals(direction)) {
            syncResult = mapper.syncDataFromFederationProviderToKeycloak(realm);
        } else if ("keycloakToFed".equals(direction)) {
            syncResult = mapper.syncDataFromKeycloakToFederationProvider(realm);
        } else {
            throw new BadRequestException("Unknown direction: " + direction);
        }

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", direction);
        eventRep.put("result", syncResult);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(eventRep).success();
        return syncResult;
    }




}
