package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.jboss.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ConditionalHttpHeaderAuthenticator implements ConditionalAuthenticator {

    public static final ConditionalHttpHeaderAuthenticator SINGLETON = new ConditionalHttpHeaderAuthenticator();
    private static final Logger logger = Logger.getLogger(ConditionalHttpHeaderAuthenticator.class);

    public boolean containsMatchingRequestHeader(MultivaluedMap<String, String> requestHeaders, String headerPattern) {
        if (headerPattern == null) {
            logger.debugv("The metching request header pattern are <null>!");
            return false;
        }

        Pattern pattern = Pattern.compile(headerPattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {

            String key = entry.getKey();

            for (String value : entry.getValue()) {

                String headerEntry = key.trim() + ": " + value.trim();

                if (pattern.matcher(headerEntry).matches()) {
                    logger.debugv("Pattern {0} matches header entry {1}", headerPattern, headerEntry);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        if(config == null){
            return false;
        }

        MultivaluedMap<String, String> requestHeaders = context.getHttpRequest().getHttpHeaders().getRequestHeaders();
        String headerPattern = config.get(ConditionalHttpHeaderAuthenticatorFactory.HTTP_HEADER_PATTERN);
        boolean negateOutcome =  Boolean.parseBoolean(config.get(ConditionalHttpHeaderAuthenticatorFactory.NEGATE_OUTCOME));

        return (negateOutcome != containsMatchingRequestHeader(requestHeaders, headerPattern));
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Not used
    }

    @Override
    public void close() {
        // Does nothing
    }
}