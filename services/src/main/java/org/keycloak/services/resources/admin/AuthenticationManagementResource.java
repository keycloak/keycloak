package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

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
}