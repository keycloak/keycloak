package org.keycloak.mappers;

import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.UserFederationMapperSyncConfigRepresentation;

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
     * Specifies if mapper supports sync data from federation storage to keycloak and viceversa.
     * Also specifies messages to be displayed in admin console UI (For example "Sync roles from LDAP" etc)
     *
     * @return syncConfig representation
     */
    UserFederationMapperSyncConfigRepresentation getSyncConfig();

    /**
     * Called when instance of mapperModel is created for this factory through admin endpoint
     *
     * @param mapperModel
     * @throws MapperConfigValidationException if configuration provided in mapperModel is not valid
     */
    void validateConfig(RealmModel realm, UserFederationMapperModel mapperModel) throws MapperConfigValidationException;

    /**
     * Used to detect what are default values for ProviderConfigProperties specified during mapper creation
     *
     * @return
     */
    Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel);

}
