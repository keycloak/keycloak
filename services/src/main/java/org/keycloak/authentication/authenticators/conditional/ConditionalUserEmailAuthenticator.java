package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;

import java.util.Arrays;
import java.util.List;

public class ConditionalUserEmailAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalUserEmailAuthenticator SINGLETON = new ConditionalUserEmailAuthenticator();

    protected static final String DOMAINS = "domains";
    protected static final String EMAILS = "emails";

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        
        String username = (String) formData.getFirst("username");

        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (username != null && authConfig!=null && authConfig.getConfig()!=null) {

            if (authConfig.getConfig().containsKey(EMAILS)) {
                List<String> emails = Arrays.asList(
                        authConfig.getConfig().get(EMAILS).split("##"));
                
                if (emails.contains(username)) {
                    return true;
                }
            }
    
            if (authConfig.getConfig().containsKey(DOMAINS)) {
                List<String> domains = Arrays.asList(
                        authConfig.getConfig().get(DOMAINS).split("##"));
                
                return domains.contains(username.substring(1 + username.indexOf("@")));
            }

        }
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used
    }

    @Override
    public boolean requiresUser() {
        return false;
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
