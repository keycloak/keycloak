package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @author Bill Burke
 */
public class AuthenticationManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;
    @Context
    private UriInfo uriInfo;

    private static Logger logger = Logger.getLogger(AuthenticationManagementResource.class);

    public AuthenticationManagementResource(RealmModel realm, KeycloakSession session, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.auth.init(RealmAuth.Resource.REALM);
        this.adminEvent = adminEvent;
    }

    public static class AuthenticationExecutionRepresentation {
        protected String id;
        protected String referenceType;
        protected String requirement;
        protected List<String> requirementChoices;
        protected Boolean configurable;
        protected Boolean subFlow;
        protected String providerId;
        protected String authenticationConfig;

        public String getId() {
            return id;
        }

        public void setId(String execution) {
            this.id = execution;
        }

        public String getReferenceType() {
            return referenceType;
        }

        public void setReferenceType(String referenceType) {
            this.referenceType = referenceType;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }

        public List<String> getRequirementChoices() {
            return requirementChoices;
        }

        public void setRequirementChoices(List<String> requirementChoices) {
            this.requirementChoices = requirementChoices;
        }

        public Boolean getConfigurable() {
            return configurable;
        }

        public void setConfigurable(Boolean configurable) {
            this.configurable = configurable;
        }

        public Boolean getSubFlow() {
            return subFlow;
        }

        public void setSubFlow(Boolean subFlow) {
            this.subFlow = subFlow;
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getAuthenticationConfig() {
            return authenticationConfig;
        }

        public void setAuthenticationConfig(String authenticationConfig) {
            this.authenticationConfig = authenticationConfig;
        }
    }

    @Path("/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuthenticationFlowModel> getFlows() {
        this.auth.requireView();
        List<AuthenticationFlowModel> flows = new LinkedList<>();
        for (AuthenticationFlowModel flow : realm.getAuthenticationFlows()) {
            if (flow.isTopLevel()) {
                flows.add(flow);
            }
        }
        return flows;
    }

    @Path("/flows/{flowAlias}/executions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecutions(@PathParam("flowAlias") String flowAlias) {
        this.auth.requireView();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            return Response.status(NOT_FOUND).build();
        }
        List<AuthenticationExecutionRepresentation> result = new LinkedList<>();
        List<AuthenticationExecutionModel> executions = AuthenticatorUtil.getEnabledExecutionsRecursively(realm, flow.getId());
        for (AuthenticationExecutionModel execution : executions) {
            AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
            rep.setSubFlow(false);
            rep.setRequirementChoices(new LinkedList<String>());
            if (execution.isAutheticatorFlow()) {
                AuthenticationFlowModel flowRef = realm.getAuthenticationFlowById(execution.getFlowId());
                if (AuthenticationFlow.BASIC_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                } else if (AuthenticationFlow.FORM_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                    rep.setProviderId(execution.getAuthenticator());
                    rep.setAuthenticationConfig(execution.getAuthenticatorConfig());

                }
                rep.setReferenceType(flowRef.getAlias());
                rep.setConfigurable(false);
                rep.setId(execution.getId());
                rep.setRequirement(execution.getRequirement().name());
                result.add(rep);
            } else {
                if (!flow.getId().equals(execution.getParentFlow())) {
                    rep.setSubFlow(true);
                }
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = getConfigurableAuthenticatorFactory(providerId);
                rep.setReferenceType(factory.getDisplayType());
                rep.setConfigurable(factory.isConfigurable());
                for (AuthenticationExecutionModel.Requirement choice : factory.getRequirementChoices()) {
                    rep.getRequirementChoices().add(choice.name());
                }
                rep.setId(execution.getId());
                rep.setRequirement(execution.getRequirement().name());
                rep.setProviderId(execution.getAuthenticator());
                rep.setAuthenticationConfig(execution.getAuthenticatorConfig());
                result.add(rep);

            }

        }
        return Response.ok(result).build();
    }

    public ConfigurableAuthenticatorFactory getConfigurableAuthenticatorFactory(String providerId) {
        ConfigurableAuthenticatorFactory factory = (AuthenticatorFactory)session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, providerId);
        if (factory == null) {
            factory = (FormActionFactory)session.getKeycloakSessionFactory().getProviderFactory(FormAction.class, providerId);
        }
        return factory;
    }

    @Path("/flows/{flowAlias}/executions")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateExecutions(@PathParam("flowAlias") String flowAlias, AuthenticationExecutionRepresentation rep) {
        this.auth.requireManage();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            throw new NotFoundException("flow not found");
        }

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(rep.getId());
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        if (!model.getRequirement().name().equals(rep.getRequirement())) {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            realm.updateAuthenticatorExecution(model);
        }
    }

    @Path("/executions/{executionId}/config")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newExecutionConfig(@PathParam("executionId") String execution, AuthenticatorConfigModel config) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        config = realm.addAuthenticatorConfig(config);
        model.setAuthenticatorConfig(config.getId());
        realm.updateAuthenticatorExecution(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    @Path("/executions/{executionId}/config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigModel getAuthenticatorConfig(@PathParam("executionId") String execution,@PathParam("id") String id) {
        this.auth.requireView();
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return config;
    }


    public static class RequiredActionProviderRepresentation {
        private String alias;
        private String name;
        private boolean enabled;
        private boolean defaultAction;
        private Map<String, String> config = new HashMap<String, String>();

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDefaultAction() {
            return defaultAction;
        }

        public void setDefaultAction(boolean defaultAction) {
            this.defaultAction = defaultAction;
        }

        public Map<String, String> getConfig() {
            return config;
        }

        public void setConfig(Map<String, String> config) {
            this.config = config;
        }
    }


    @Path("required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RequiredActionProviderRepresentation> getRequiredActions() {
        List<RequiredActionProviderRepresentation> list = new LinkedList<>();
        for (RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
            RequiredActionProviderRepresentation rep = toRepresentation(model);
            list.add(rep);
        }
        return list;
    }

    public static RequiredActionProviderRepresentation toRepresentation(RequiredActionProviderModel model) {
        RequiredActionProviderRepresentation rep = new RequiredActionProviderRepresentation();
        rep.setAlias(model.getAlias());
        rep.setName(model.getName());
        rep.setDefaultAction(model.isDefaultAction());
        rep.setEnabled(model.isEnabled());
        rep.setConfig(model.getConfig());
        return rep;
    }

    @Path("required-actions/{alias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public RequiredActionProviderRepresentation getRequiredAction(@PathParam("alias") String alias) {
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action: " + alias);
        }
        return toRepresentation(model);
    }


    @Path("required-actions/{alias}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRequiredAction(@PathParam("alias") String alias, RequiredActionProviderRepresentation rep) {
        this.auth.requireManage();
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action: " + alias);
        }
        RequiredActionProviderModel update = new RequiredActionProviderModel();
        update.setId(model.getId());
        update.setName(rep.getName());
        update.setAlias(rep.getAlias());
        update.setProviderId(model.getProviderId());
        update.setDefaultAction(rep.isDefaultAction());
        update.setEnabled(rep.isEnabled());
        update.setConfig(rep.getConfig());
        realm.updateRequiredActionProvider(update);
    }

    @Path("required-actions/{alias}")
    @DELETE
    public void updateRequiredAction(@PathParam("alias") String alias) {
        this.auth.requireManage();
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action: " + alias);
        }
        realm.removeRequiredActionProvider(model);
    }

    public class AuthenticatorConfigDescription {
        protected String name;
        protected String providerId;
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

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
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


    @Path("config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigDescription getAuthenticatorConfigDescription(@PathParam("providerId") String providerId) {
        this.auth.requireView();
        ConfigurableAuthenticatorFactory factory = getConfigurableAuthenticatorFactory(providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find authenticator provider");
        }
        AuthenticatorConfigDescription rep = new AuthenticatorConfigDescription();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayType());
        rep.setHelpText(factory.getHelpText());
        rep.setProperties(new LinkedList<ConfigPropertyRepresentation>());
        List<ProviderConfigProperty> configProperties = factory.getConfigProperties();
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
            propRep.setName(prop.getName());
            propRep.setLabel(prop.getLabel());
            propRep.setType(prop.getType());
            propRep.setDefaultValue(prop.getDefaultValue());
            propRep.setHelpText(prop.getHelpText());
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    @Path("config")
    @POST
    @NoCache
    public Response createAuthenticatorConfig(AuthenticatorConfigModel config) {
        this.auth.requireManage();
        config = realm.addAuthenticatorConfig(config);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    @Path("config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigModel getAuthenticatorConfig(@PathParam("id") String id) {
        this.auth.requireView();
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return config;
    }
    @Path("config/{id}")
    @DELETE
    @NoCache
    public void removeAuthenticatorConfig(@PathParam("id") String id) {
        this.auth.requireManage();
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        List<AuthenticationFlowModel> flows = new LinkedList<>();
        for (AuthenticationFlowModel flow : realm.getAuthenticationFlows()) {
            for (AuthenticationExecutionModel exe : realm.getAuthenticationExecutions(flow.getId())) {
                if (id.equals(exe.getAuthenticatorConfig())) {
                    exe.setAuthenticatorConfig(null);
                    realm.updateAuthenticatorExecution(exe);
                }
            }
        }

        realm.removeAuthenticatorConfig(config);
    }
    @Path("config/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateAuthenticatorConfig(@PathParam("id") String id, AuthenticatorConfigModel config) {
        this.auth.requireManage();
        AuthenticatorConfigModel exists = realm.getAuthenticatorConfigById(id);
        if (exists == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        exists.setAlias(config.getAlias());
        exists.setConfig(config.getConfig());
        realm.updateAuthenticatorConfig(exists);
    }




}