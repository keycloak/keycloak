package org.keycloak.representations.admin.v2.validators;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation.Flow;
import org.keycloak.representations.admin.v2.validation.ConfidentialFlowsRequireAuth;

public class ConfidentialFlowsRequireAuthValidator implements ConstraintValidator<ConfidentialFlowsRequireAuth, OIDCClientRepresentation> {

    private static final Set<Flow> CONFIDENTIAL_FLOWS = Set.of(Flow.SERVICE_ACCOUNT, Flow.TOKEN_EXCHANGE);

    @Override
    public boolean isValid(OIDCClientRepresentation representation, ConstraintValidatorContext context) {
        Set<Flow> loginFlows = representation.getLoginFlows();
        if (loginFlows == null || loginFlows.isEmpty()) {
            return true;
        }
        if (representation.getAuth() != null) {
            return true;
        }
        for (Flow flow : CONFIDENTIAL_FLOWS) {
            if (loginFlows.contains(flow)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                flow + " requires a confidential client (auth must be specified)")
                        .addPropertyNode("loginFlows")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
