package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.DefaultAuthenticationFlow;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormAuthenticationFlow;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.utils.CredentialHelper;

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
import java.util.Collections;
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
        protected String requirement;
        protected String displayName;
        protected List<String> requirementChoices;
        protected Boolean configurable;
        protected Boolean authenticationFlow;
        protected String providerId;
        protected String authenticationConfig;
        protected String flowId;
        protected int level;
        protected int index;

        public String getId() {
            return id;
        }

        public void setId(String execution) {
            this.id = execution;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
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

        public Boolean getAuthenticationFlow() {
            return authenticationFlow;
        }

        public void setAuthenticationFlow(Boolean authenticationFlow) {
            this.authenticationFlow = authenticationFlow;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getFlowId() {
            return flowId;
        }

        public void setFlowId(String flowId) {
            this.flowId = flowId;
        }
    }

    @Path("/form-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getFormProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(FormAuthenticator.class);
        return buildProviderMetadata(factories);
    }

    @Path("/authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getAuthenticatorProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(Authenticator.class);
        return buildProviderMetadata(factories);
    }

    @Path("/client-authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getClientAuthenticatorProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(ClientAuthenticator.class);
        return buildProviderMetadata(factories);
    }

    public List<Map<String, Object>> buildProviderMetadata(List<ProviderFactory> factories) {
        List<Map<String, Object>> providers = new LinkedList<>();
        for (ProviderFactory factory : factories) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", factory.getId());
            ConfigurableAuthenticatorFactory configured = (ConfigurableAuthenticatorFactory)factory;
            data.put("description", configured.getHelpText());
            data.put("displayName", configured.getDisplayType());

            providers.add(data);
        }
        return providers;
    }

    @Path("/form-action-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getFormActionProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(FormAction.class);
        return buildProviderMetadata(factories);
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

    @Path("/flows")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFlow(AuthenticationFlowModel model) {
        this.auth.requireManage();

        if (realm.getFlowByAlias(model.getAlias()) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        realm.addAuthenticationFlow(model);
        return Response.status(201).build();

    }

    @Path("/flows/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowModel getFlow(@PathParam("id") String id) {
        this.auth.requireView();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }
        return flow;
    }

    @Path("/flows/{id}")
    @DELETE
    @NoCache
    public void deleteFlow(@PathParam("id") String id) {
        this.auth.requireView();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }
        if (flow.isBuiltIn()) {
            throw new BadRequestException("Can't delete built in flow");
        }
        realm.removeAuthenticationFlow(flow);
    }

    @Path("/flows/{flowAlias}/copy")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response copy(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        this.auth.requireManage();

        String newName = data.get("newName");
        if (realm.getFlowByAlias(newName) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            return Response.status(NOT_FOUND).build();
        }
        AuthenticationFlowModel copy = new AuthenticationFlowModel();
        copy.setAlias(newName);
        copy.setDescription(flow.getDescription());
        copy.setProviderId(flow.getProviderId());
        copy.setBuiltIn(false);
        copy.setTopLevel(flow.isTopLevel());
        copy = realm.addAuthenticationFlow(copy);
        copy(newName, flow, copy);

        return Response.status(201).build();

    }

    protected void copy(String newName, AuthenticationFlowModel from, AuthenticationFlowModel to) {
        for (AuthenticationExecutionModel execution : realm.getAuthenticationExecutions(from.getId())) {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                AuthenticationFlowModel copy = new AuthenticationFlowModel();
                copy.setAlias(newName + " " + subFlow.getAlias());
                copy.setDescription(subFlow.getDescription());
                copy.setProviderId(subFlow.getProviderId());
                copy.setBuiltIn(false);
                copy.setTopLevel(false);
                copy = realm.addAuthenticationFlow(copy);
                execution.setFlowId(copy.getId());
                copy(newName, subFlow, copy);
            }
            execution.setId(null);
            execution.setParentFlow(to.getId());
            realm.addAuthenticatorExecution(execution);
        }
    }

    @Path("/flows/{flowAlias}/executions/flow")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void addExecutionFlow(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        this.auth.requireManage();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw new BadRequestException("Parent flow doesn't exists");
        }
        String alias = data.get("alias");
        String type = data.get("type");
        String provider = data.get("provider");
        String description = data.get("description");


        AuthenticationFlowModel newFlow = realm.getFlowByAlias(alias);
        if (newFlow != null) {
            throw new BadRequestException("New flow alias name already exists");
        }
        newFlow = new AuthenticationFlowModel();
        newFlow.setAlias(alias);
        newFlow.setDescription(description);
        newFlow.setProviderId(type);
        newFlow = realm.addAuthenticationFlow(newFlow);
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setFlowId(newFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticatorFlow(true);
        execution.setAuthenticator(provider);
        realm.addAuthenticatorExecution(execution);
    }

    @Path("/flows/{flowAlias}/executions/execution")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void addExecution(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        this.auth.requireManage();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw new BadRequestException("Parent flow doesn't exists");
        }
        String provider = data.get("provider");


        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(provider);
        realm.addAuthenticatorExecution(execution);
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

        int level = 0;

        recurseExecutions(flow, result, level);
        return Response.ok(result).build();
    }

    public void recurseExecutions(AuthenticationFlowModel flow, List<AuthenticationExecutionRepresentation> result, int level) {
        int index = 0;
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(flow.getId());
        for (AuthenticationExecutionModel execution : executions) {
            AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
            rep.setLevel(level);
            rep.setIndex(index++);
            rep.setRequirementChoices(new LinkedList<String>());
            if (execution.isAuthenticatorFlow()) {
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
                } else if (AuthenticationFlow.CLIENT_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                }
                rep.setDisplayName(flowRef.getAlias());
                rep.setConfigurable(false);
                rep.setId(execution.getId());
                rep.setAuthenticationFlow(execution.isAuthenticatorFlow());
                rep.setRequirement(execution.getRequirement().name());
                rep.setFlowId(execution.getFlowId());
                result.add(rep);
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                recurseExecutions(subFlow, result, level + 1);
            } else {
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
                rep.setDisplayName(factory.getDisplayType());
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

    @Path("/executions")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addExecution(AuthenticationExecutionModel model) {
        this.auth.requireManage();
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add execution to a built in flow");
        }
        int priority = 0;
        List<AuthenticationExecutionModel> executions = getSortedExecutions(parentFlow);
        for (AuthenticationExecutionModel execution : executions) {
            priority = execution.getPriority();
        }
        if (priority > 0) priority += 10;
        model.setPriority(priority);
        model = realm.addAuthenticatorExecution(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    public AuthenticationFlowModel getParentFlow(AuthenticationExecutionModel model) {
        if (model.getParentFlow() == null) {
            throw new BadRequestException("parent flow not set on new execution");
        }
        AuthenticationFlowModel parentFlow = realm.getAuthenticationFlowById(model.getParentFlow());
        if (parentFlow == null) {
            throw new BadRequestException("execution parent flow does not exist");

        }
        return parentFlow;
    }

    @Path("/executions/{executionId}/raise-priority")
    @POST
    @NoCache
    public void raisePriority(@PathParam("executionId") String execution) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to modify execution in a built in flow");
        }
        List<AuthenticationExecutionModel> executions = getSortedExecutions(parentFlow);
        AuthenticationExecutionModel previous = null;
        for (AuthenticationExecutionModel exe : executions) {
            if (exe.getId().equals(model.getId())) {
                break;
            }
            previous = exe;

        }
        if (previous == null) return;
        int tmp = previous.getPriority();
        previous.setPriority(model.getPriority());
        realm.updateAuthenticatorExecution(previous);
        model.setPriority(tmp);
        realm.updateAuthenticatorExecution(model);
    }

    public List<AuthenticationExecutionModel> getSortedExecutions(AuthenticationFlowModel parentFlow) {
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(parentFlow.getId());
        Collections.sort(executions, AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
        return executions;
    }

    @Path("/executions/{executionId}/lower-priority")
    @POST
    @NoCache
    public void lowerPriority(@PathParam("executionId") String execution) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to modify execution in a built in flow");
        }
        List<AuthenticationExecutionModel> executions = getSortedExecutions(parentFlow);
        int i = 0;
        for (i = 0; i < executions.size(); i++) {
            if (executions.get(i).getId().equals(model.getId())) {
                break;
            }
        }
        if (i + 1 >= executions.size()) return;
        AuthenticationExecutionModel next = executions.get(i + 1);
        int tmp = model.getPriority();
        model.setPriority(next.getPriority());
        realm.updateAuthenticatorExecution(model);
        next.setPriority(tmp);
        realm.updateAuthenticatorExecution(next);
    }


    @Path("/executions/{executionId}")
    @DELETE
    @NoCache
    public void removeExecution(@PathParam("executionId") String execution) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to remove execution from a built in flow");
        }
        realm.removeAuthenticatorExecution(model);
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

    @Path("unregistered-required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<Map<String, String>> getUnregisteredRequiredActions() {
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class);
        List<Map<String, String>> unregisteredList = new LinkedList<>();
        for (ProviderFactory factory : factories) {
            RequiredActionFactory requiredActionFactory = (RequiredActionFactory) factory;
            boolean found = false;
            for (RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
                if (model.getProviderId().equals(factory.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Map<String, String> data = new HashMap<>();
                data.put("name", requiredActionFactory.getDisplayText());
                data.put("providerId", requiredActionFactory.getId());
                unregisteredList.add(data);
            }

        }
        return unregisteredList;
    }

    @Path("register-required-action")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void registereRequiredAction(Map<String, String> data) {
        String providerId = data.get("providerId");
        String name = data.get("name");
        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(providerId);
        requiredAction.setName(name);
        requiredAction.setProviderId(providerId);
        requiredAction.setDefaultAction(false);
        requiredAction.setEnabled(true);
        realm.addRequiredActionProvider(requiredAction);
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
            throw new NotFoundException("Failed to find required action");
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
            throw new NotFoundException("Failed to find required action");
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
            throw new NotFoundException("Failed to find required action.");
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
        ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
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
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    private ConfigPropertyRepresentation getConfigPropertyRep(ProviderConfigProperty prop) {
        ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
        propRep.setName(prop.getName());
        propRep.setLabel(prop.getLabel());
        propRep.setType(prop.getType());
        propRep.setDefaultValue(prop.getDefaultValue());
        propRep.setHelpText(prop.getHelpText());
        return propRep;
    }


    @Path("per-client-config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String, List<ConfigPropertyRepresentation>> getPerClientConfigDescription() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(ClientAuthenticator.class);

        Map<String, List<ConfigPropertyRepresentation>> toReturn = new HashMap<>();
        for (ProviderFactory clientAuthenticatorFactory : factories) {
            String providerId = clientAuthenticatorFactory.getId();
            ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
            ClientAuthenticatorFactory clientAuthFactory = (ClientAuthenticatorFactory) factory;
            List<ProviderConfigProperty> perClientConfigProps = clientAuthFactory.getConfigPropertiesPerClient();
            List<ConfigPropertyRepresentation> result = new LinkedList<>();
            for (ProviderConfigProperty prop : perClientConfigProps) {
                ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
                result.add(propRep);
            }

            toReturn.put(providerId, result);
        }

        return toReturn;
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