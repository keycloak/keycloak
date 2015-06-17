package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.provider.ProviderFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    private static Logger logger = Logger.getLogger(AuthenticationManagementResource.class);

    public AuthenticationManagementResource(RealmModel realm, KeycloakSession session, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.auth.init(RealmAuth.Resource.IDENTITY_PROVIDER);
        this.adminEvent = adminEvent;
    }

    public static class AuthenticationExecutionRepresentation {
        protected String execution;
        protected String referenceType;
        protected String requirement;
        protected List<String> requirementChoices;
        protected Boolean configurable;
        protected Boolean subFlow;

        public String getExecution() {
            return execution;
        }

        public void setExecution(String execution) {
            this.execution = execution;
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
    }

    @Path("/flow/{flowAlias}/executions")
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
                AuthenticationFlowModel flowRef = realm.getAuthenticationFlowById(execution.getAuthenticator());
                rep.setReferenceType(flowRef.getAlias());
                rep.setExecution(execution.getId());
                rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                rep.setConfigurable(false);
                rep.setExecution(execution.getId());
                rep.setRequirement(execution.getRequirement().name());
                result.add(rep);
            } else {
                if (!flow.getId().equals(execution.getParentFlow())) {
                    rep.setSubFlow(true);
                }
                AuthenticatorModel authenticator = realm.getAuthenticatorById(execution.getAuthenticator());
                AuthenticatorFactory factory = (AuthenticatorFactory)session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, authenticator.getProviderId());
                if (factory.getReferenceType() == null) continue;
                rep.setReferenceType(factory.getReferenceType());
                rep.setConfigurable(factory.isConfigurable());
                for (AuthenticationExecutionModel.Requirement choice : factory.getRequirementChoices()) {
                    rep.getRequirementChoices().add(choice.name());
                }
                rep.setExecution(execution.getId());
                rep.setRequirement(execution.getRequirement().name());
                result.add(rep);

            }

        }
        return Response.ok(result).build();
    }

    @Path("/flow/{flowAlias}/executions")
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

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(rep.getExecution());
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        if (!model.getRequirement().name().equals(rep.getRequirement())) {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            realm.updateAuthenticatorExecution(model);
        }
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
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action: " + alias);
        }
        realm.removeRequiredActionProvider(model);
    }


}