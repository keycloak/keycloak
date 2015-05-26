package org.keycloak.mappers;

import java.util.List;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapperFactory extends ProviderFactory<UserFederationMapper>, ConfiguredProvider {

    /**
     * Refers to providerName (type) of the federation provider, which this mapper can be used for. For example "ldap" or "kerberos"
     *
     * @return providerName
     */
    String getFederationProviderType();

    String getDisplayCategory();
    String getDisplayType();

    /**
     * Called when instance of mapperModel is created for this factory through admin endpoint
     *
     * @param mapperModel
     * @throws MapperConfigValidationException if configuration provided in mapperModel is not valid
     */
    void validateConfig(UserFederationMapperModel mapperModel) throws MapperConfigValidationException;

    // TODO: Remove this and add realm to the method on ConfiguredProvider?
    List<ProviderConfigProperty> getConfigProperties(RealmModel realm);
}
