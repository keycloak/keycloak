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
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationValidatingProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserFederationProviderFactoryRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.utils.CredentialHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserFederationProvidersResource {
    private static final Logger logger = Logger.getLogger(UserFederationProvidersResource.class);

    protected RealmModel realm;

    protected  RealmAuth auth;

    protected AdminEventBuilder adminEvent;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public UserFederationProvidersResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.USER_FEDERATION_PROVIDER);

        auth.init(RealmAuth.Resource.REALM);
    }

    /**
     * Automatically add "kerberos" to required realm credentials if it's supported by saved provider
     *
     * @param realm
     * @param model
     * @return true if kerberos credentials were added
     */
    public static boolean checkKerberosCredential(KeycloakSession session, RealmModel realm, UserFederationProviderModel model) {
        String allowKerberosCfg = model.getConfig().get(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION);
        if (Boolean.valueOf(allowKerberosCfg)) {
            CredentialHelper.setOrReplaceAuthenticationRequirement(session, realm, CredentialRepresentation.KERBEROS,
                    AuthenticationExecutionModel.Requirement.ALTERNATIVE, AuthenticationExecutionModel.Requirement.DISABLED);
            return true;
        }

        return false;
    }

    public static void validateFederationProviderConfig(KeycloakSession session, RealmAuth auth, RealmModel realm, UserFederationProviderModel model) {
        UserFederationProviderFactory providerFactory = KeycloakModelUtils.getFederationProviderFactory(session, model);
        if (providerFactory instanceof UserFederationValidatingProviderFactory) {
            try {
                ((UserFederationValidatingProviderFactory) providerFactory).validateConfig(realm, model);
            } catch (FederationConfigValidationException fcve) {
                logger.error(fcve.getMessage());
                Properties messages = AdminRoot.getMessages(session, realm, auth.getAuth().getToken().getLocale());
                throw new ErrorResponseException(fcve.getMessage(), MessageFormat.format(messages.getProperty(fcve.getMessage(), fcve.getMessage()), fcve.getParameters()),
                        Response.Status.BAD_REQUEST);
            }
        }
    }

    /**
     * Get available provider factories
     *
     * Returns a list of available provider factories.
     *
     * @return
     */
    @GET
    @NoCache
    @Path("providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserFederationProviderFactoryRepresentation> getProviders() {
        auth.requireView();

        List<UserFederationProviderFactoryRepresentation> providers = new LinkedList<UserFederationProviderFactoryRepresentation>();
        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(UserFederationProvider.class)) {
            UserFederationProviderFactoryRepresentation rep = new UserFederationProviderFactoryRepresentation();
            rep.setId(factory.getId());
            rep.setOptions(((UserFederationProviderFactory)factory).getConfigurationOptions());
            providers.add(rep);
        }
        return providers;
    }

    /**
     * Get factory with given id
     *
     * @return
     */
    @GET
    @NoCache
    @Path("providers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationProviderFactoryRepresentation getProvider(@PathParam("id") String id) {
        auth.requireView();

        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(UserFederationProvider.class)) {
            if (!factory.getId().equals(id)) {
                continue;
            }

            if (factory instanceof ConfiguredProvider) {

                UserFederationProviderFactoryDescription rep = new UserFederationProviderFactoryDescription();
                rep.setId(factory.getId());

                ConfiguredProvider cp = (ConfiguredProvider) factory;
                rep.setHelpText(cp.getHelpText());
                rep.setProperties(toConfigPropertyRepresentationList(cp.getConfigProperties()));

                return rep;
            }

            UserFederationProviderFactoryRepresentation rep = new UserFederationProviderFactoryRepresentation();
            rep.setId(factory.getId());
            rep.setOptions(((UserFederationProviderFactory) factory).getConfigurationOptions());

            return rep;
        }
        throw new NotFoundException("Could not find provider");
    }

    /**
     * Create a provider
     *
     * @param rep
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProviderInstance(UserFederationProviderRepresentation rep) {
        auth.requireManage();

        try {
            String displayName = rep.getDisplayName();
            if (displayName != null && displayName.trim().equals("")) {
                displayName = null;
            }

            UserFederationProviderModel tempModel = new UserFederationProviderModel(null, rep.getProviderName(), rep.getConfig(), rep.getPriority(), displayName, rep.getFullSyncPeriod(), rep.getChangedSyncPeriod(), rep.getLastSync());
            validateFederationProviderConfig(session, auth, realm, tempModel);

            UserFederationProviderModel model = realm.addUserFederationProvider(rep.getProviderName(), rep.getConfig(), rep.getPriority(), displayName,
                    rep.getFullSyncPeriod(), rep.getChangedSyncPeriod(), rep.getLastSync());
            new UsersSyncManager().notifyToRefreshPeriodicSync(session, realm, model, false);
            boolean kerberosCredsAdded = checkKerberosCredential(session, realm, model);
            if (kerberosCredsAdded) {
                ServicesLogger.LOGGER.addedKerberosToRealmCredentials();
            }

            rep.setId(model.getId());
            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, model.getId()).representation(rep).success();

            return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("Federation provider exists with same name.");
        } catch (ModelException me){
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.error("Could not create federation provider.", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get configured providers
     *
     * @return
     */
    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<UserFederationProviderRepresentation> getUserFederationInstances() {
        auth.requireView();

        List<UserFederationProviderRepresentation> reps = new LinkedList<UserFederationProviderRepresentation>();
        for (UserFederationProviderModel model : realm.getUserFederationProviders()) {
            UserFederationProviderRepresentation rep = ModelToRepresentation.toRepresentation(model);
            reps.add(rep);
        }
        return reps;
    }

    @Path("instances/{id}")
    public UserFederationProviderResource getUserFederationInstance(@PathParam("id") String id) {
        this.auth.requireView();

        UserFederationProviderModel model = KeycloakModelUtils.findUserFederationProviderById(id, realm);
        UserFederationProviderResource instanceResource = new UserFederationProviderResource(session, realm, this.auth, model, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(instanceResource);
        return instanceResource;
    }

    // TODO: This endpoint exists, so that admin console can lookup userFederation provider OR userStorage provider by federationLink.
    // TODO: Endpoint should be removed once UserFederation SPI is removed as fallback is not needed anymore than
    @GET
    @Path("instances-with-fallback/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String, String> getUserFederationInstanceWithFallback(@PathParam("id") String id) {
        this.auth.requireView();

        Map<String, String> result = new HashMap<>();
        UserFederationProviderModel model = KeycloakModelUtils.findUserFederationProviderById(id, realm);
        if (model != null) {
            result.put("federationLinkName", model.getDisplayName());
            result.put("federationLink", "#/realms/" + realm.getName() + "/user-federation/providers/" + model.getProviderName() + "/" + model.getId());
            return result;
        } else {
            ComponentModel userStorage = KeycloakModelUtils.findUserStorageProviderById(id, realm);
            if (userStorage != null) {
                result.put("federationLinkName", userStorage.getName());
                result.put("federationLink", "#/realms/" + realm.getName() + "/user-storage/providers/" + userStorage.getProviderId() + "/" + userStorage.getId());
                return result;
            } else {
                throw new NotFoundException("Could not find federation provider or userStorage provider");
            }
        }
    }


    private ConfigPropertyRepresentation toConfigPropertyRepresentation(ProviderConfigProperty prop) {
        return ModelToRepresentation.toRepresentation(prop);
    }

    private List<ConfigPropertyRepresentation> toConfigPropertyRepresentationList(List<ProviderConfigProperty> props) {

        List<ConfigPropertyRepresentation> reps = new ArrayList<>(props.size());
        for(ProviderConfigProperty prop : props){
            reps.add(toConfigPropertyRepresentation(prop));
        }

        return reps;
    }


    public static class UserFederationProviderFactoryDescription extends UserFederationProviderFactoryRepresentation {

        protected String name;

        protected String helpText;

        protected List<ConfigPropertyRepresentation> properties;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHelpText() {
            return helpText;
        }

        public void setHelpText(String helpText) {
            this.helpText = helpText;
        }

        public List<ConfigPropertyRepresentation> getProperties() {
            return properties;
        }

        public void setProperties(List<ConfigPropertyRepresentation> properties) {
            this.properties = properties;
        }
    }
}
