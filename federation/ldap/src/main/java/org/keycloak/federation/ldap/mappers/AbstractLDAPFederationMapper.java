package org.keycloak.federation.ldap.mappers;

import org.keycloak.models.UserFederationMapperModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapper implements LDAPFederationMapper {

    @Override
    public void close() {

    }

    protected boolean parseBooleanParameter(UserFederationMapperModel mapperModel, String paramName) {
        String paramm = mapperModel.getConfig().get(paramName);
        return Boolean.parseBoolean(paramm);
    }
}
