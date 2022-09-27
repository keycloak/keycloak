package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

/**
 * An {@link ConditionalAuthenticatorFactory} for {@link ConditionalHttpHeaderAuthenticator}s.
 *
 * @author <a href="mailto:preisner@puzzle-itc.de">Sebastian Preisner</a>
 */
public class ConditionalHttpHeaderAuthenticatorFactory implements ConditionalAuthenticatorFactory {
    public static final String PROVIDER_ID = "conditional-http-header";

    public static final String HTTP_HEADER_PATTERN = "search_pattern";
    public static final String NEGATE_OUTCOME = "negate_outcome";

    @Override
    public void init(Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition - request header";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Flow is executed olny if HTTP request header matches supplied regular expression.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        ProviderConfigProperty HttpHeaderPattern = new ProviderConfigProperty();
        HttpHeaderPattern.setType(ProviderConfigProperty.STRING_TYPE);
        HttpHeaderPattern.setName(HTTP_HEADER_PATTERN);
        HttpHeaderPattern.setLabel("HTTP Header Pattern");
        HttpHeaderPattern.setHelpText("If a HTTP request header matches the given pattern the condition will be true." +
                "Can be used to specify trusted networks via: X-Forwarded-Host: (1.2.3.4|1.2.3.5)." +
                "In this case requests from 1.2.3.4 and 1.2.3.5 come from a trusted source.");
        HttpHeaderPattern.setDefaultValue("");

        ProviderConfigProperty negateOutcome = new ProviderConfigProperty();
        negateOutcome.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutcome.setName(NEGATE_OUTCOME);
        negateOutcome.setLabel("Negate");
        negateOutcome.setHelpText("Send false if the given pattern matches.");

        return Arrays.asList(HttpHeaderPattern, negateOutcome);
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalHttpHeaderAuthenticator.SINGLETON;
    }

}