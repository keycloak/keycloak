package org.keycloak.federation.ldap.mappers;

import java.util.List;

import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullNameLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    @Override
    public String getHelpText() {
        return "Some help text - full name mapper - TODO";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public String getId() {
        return "full-name-ldap-mapper";
    }

    @Override
    public UserFederationMapper create(KeycloakSession session) {
        return new FullNameLDAPFederationMapper();
    }
}
