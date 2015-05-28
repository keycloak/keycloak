package org.keycloak.federation.ldap.mappers;

import java.util.List;

import org.keycloak.mappers.UserFederationMapperFactory;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapper implements LDAPFederationMapper {

    private final AbstractLDAPFederationMapperFactory factory;

    public AbstractLDAPFederationMapper(AbstractLDAPFederationMapperFactory factory) {
        this.factory = factory;
    }

    @Override
    public void close() {

    }

    @Override
    public UserFederationMapperFactory getFactory() {
        return factory;
    }

    @Override
    public String getHelpText() {
        return factory.getHelpText();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return factory.getBaseConfigProperties();
    }

    protected boolean parseBooleanParameter(UserFederationMapperModel mapperModel, String paramName) {
        String paramm = mapperModel.getConfig().get(paramName);
        return Boolean.parseBoolean(paramm);
    }
}
