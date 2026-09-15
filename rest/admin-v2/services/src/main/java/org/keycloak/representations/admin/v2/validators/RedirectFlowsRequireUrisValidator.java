package org.keycloak.representations.admin.v2.validators;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation.Flow;
import org.keycloak.representations.admin.v2.validation.RedirectFlowsRequireUris;

public class RedirectFlowsRequireUrisValidator implements ConstraintValidator<RedirectFlowsRequireUris, OIDCClientRepresentation> {

    private static final Set<Flow> REDIRECT_FLOWS = Set.of(Flow.STANDARD, Flow.IMPLICIT);

    @Override
    public boolean isValid(OIDCClientRepresentation representation, ConstraintValidatorContext context) {
        Set<Flow> loginFlows = representation.getLoginFlows();
        if (loginFlows == null || loginFlows.isEmpty()) {
            return true;
        }
        Set<String> redirectUris = representation.getRedirectUris();
        if (redirectUris != null && !redirectUris.isEmpty()) {
            return true;
        }
        for (Flow flow : REDIRECT_FLOWS) {
            if (loginFlows.contains(flow)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                flow + " requires at least one redirect URI")
                        .addPropertyNode("redirectUris")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
