package org.keycloak.authorization.policy.provider.time;

import java.text.SimpleDateFormat;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyProviderFactory implements PolicyProviderFactory<PolicyRepresentation> {

    private TimePolicyProvider provider = new TimePolicyProvider();

    @Override
    public String getName() {
        return "Time";
    }

    @Override
    public String getGroup() {
        return "Time Based";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer, AuthorizationProvider authorization) {
        return new TimePolicyAdminResource();
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void onCreate(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        validateConfig(policy);
    }

    private void validateConfig(Policy policy) {
        String nbf = policy.getConfig().get("nbf");
        String noa = policy.getConfig().get("noa");

        if (nbf != null && noa != null) {
            validateFormat(nbf);
            validateFormat(noa);
        }
    }

    @Override
    public void onUpdate(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        validateConfig(policy);
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {
    }

    private void validateFormat(String date) {
        try {
            new SimpleDateFormat(TimePolicyProvider.DEFAULT_DATE_PATTERN).parse(TimePolicyProvider.format(date));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse a date using format [" + date + "]");
        }
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
        return "time";
    }
}
