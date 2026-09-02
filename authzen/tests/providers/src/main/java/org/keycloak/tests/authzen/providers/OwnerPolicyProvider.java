package org.keycloak.tests.authzen.providers;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

/**
 * Grants access when the "ownerID" context attribute matches the requesting user's email.
 * Used by the AuthZen interop tests.
 */
public class OwnerPolicyProvider implements PolicyProviderFactory<PolicyRepresentation>, PolicyProvider {

    @Override
    public String getName() {
        return "Owner Policy";
    }

    @Override
    public String getGroup() {
        return "Test Suite";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return this;
    }

    @Override
    public PolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        return new PolicyRepresentation();
    }

    @Override
    public Class getRepresentationType() {
        return PolicyRepresentation.class;
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer, AuthorizationProvider authorization) {
        return null;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "owner-policy";
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Attributes contextAttributes = evaluation.getContext().getAttributes();
        Attributes identityAttributes = evaluation.getContext().getIdentity().getAttributes();

        Attributes.Entry ownerID = contextAttributes.getValue("ownerID");
        Attributes.Entry userEmail = identityAttributes.getValue("email");

        if (ownerID != null && userEmail != null) {
            String owner = ownerID.asString(0);
            String email = userEmail.asString(0);
            if (owner.equals(email)) {
                evaluation.grant();
            }
        }
    }
}
