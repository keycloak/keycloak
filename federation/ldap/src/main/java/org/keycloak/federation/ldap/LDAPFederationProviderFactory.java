/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.impl.KerberosServerSubjectAuthenticator;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.LDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.msad.MSADUserAccountControlMapperFactory;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationEventAwareProviderFactory;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserFederationValidatingProviderFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPFederationProviderFactory extends UserFederationEventAwareProviderFactory implements UserFederationValidatingProviderFactory {
    private static final Logger logger = Logger.getLogger(LDAPFederationProviderFactory.class);
    public static final String PROVIDER_NAME = LDAPConstants.LDAP_PROVIDER;

    private LDAPIdentityStoreRegistry ldapStoreRegistry;

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        throw new IllegalAccessError("Illegal to call this method");
    }

    @Override
    public LDAPFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        LDAPIdentityStore ldapIdentityStore = this.ldapStoreRegistry.getLdapStore(model);
        return new LDAPFederationProvider(this, session, model, ldapIdentityStore);
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel providerModel) throws FederationConfigValidationException {
        LDAPConfig cfg = new LDAPConfig(providerModel.getConfig());
        String customFilter = cfg.getCustomUserSearchFilter();
        LDAPUtils.validateCustomLdapFilter(customFilter);
    }

    @Override
    public void init(Config.Scope config) {
        this.ldapStoreRegistry = new LDAPIdentityStoreRegistry();
    }

    @Override
    public void close() {
        this.ldapStoreRegistry = null;
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<String> getConfigurationOptions() {
        return Collections.emptySet();
    }


    // Best effort to create appropriate mappers according to our LDAP config
    @Override
    public void onProviderModelCreated(RealmModel realm, UserFederationProviderModel newProviderModel) {
        LDAPConfig ldapConfig = new LDAPConfig(newProviderModel.getConfig());

        boolean activeDirectory = ldapConfig.isActiveDirectory();
        UserFederationProvider.EditMode editMode = ldapConfig.getEditMode();
        String readOnly = String.valueOf(editMode == UserFederationProvider.EditMode.READ_ONLY || editMode == UserFederationProvider.EditMode.UNSYNCED);
        String usernameLdapAttribute = ldapConfig.getUsernameLdapAttribute();

        String alwaysReadValueFromLDAP = String.valueOf(editMode==UserFederationProvider.EditMode.READ_ONLY || editMode== UserFederationProvider.EditMode.WRITABLE);

        UserFederationMapperModel mapperModel;
        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("username", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, usernameLdapAttribute,
                UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
        realm.addUserFederationMapper(mapperModel);

        // CN is typically used as RDN for Active Directory deployments
        if (ldapConfig.getRdnLdapAttribute().equalsIgnoreCase(LDAPConstants.CN)) {

            if (usernameLdapAttribute.equalsIgnoreCase(LDAPConstants.CN)) {

                // For AD deployments with "cn" as username, we will map "givenName" to first name
                mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                        UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                        UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                        UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                        UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                        UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
                realm.addUserFederationMapper(mapperModel);

            } else {
                if (editMode == UserFederationProvider.EditMode.WRITABLE) {

                    // For AD deployments with "sAMAccountName" as username and writable, we need to map "cn" as username as well (this is needed so we can register new users from KC into LDAP) and we will map "givenName" to first name.
                    mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                            UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                            UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                            UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                            UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                            UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
                    realm.addUserFederationMapper(mapperModel);

                    mapperModel = KeycloakModelUtils.createUserFederationMapperModel("username-cn", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                            UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                            UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                            UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                            UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                            UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
                    realm.addUserFederationMapper(mapperModel);
                } else {

                    // For read-only LDAP, we map "cn" as full name
                    mapperModel = KeycloakModelUtils.createUserFederationMapperModel("full name", newProviderModel.getId(), FullNameLDAPFederationMapperFactory.PROVIDER_ID,
                            FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                            FullNameLDAPFederationMapper.READ_ONLY, readOnly,
                            FullNameLDAPFederationMapper.WRITE_ONLY, "false");
                    realm.addUserFederationMapper(mapperModel);
                }
            }
        } else {
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                    UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                    UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                    UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                    UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                    UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
            realm.addUserFederationMapper(mapperModel);
        }

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("last name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.LAST_NAME,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.SN,
                UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
        realm.addUserFederationMapper(mapperModel);

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("email", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.EMAIL,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.EMAIL,
                UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "false");
        realm.addUserFederationMapper(mapperModel);

        String createTimestampLdapAttrName = activeDirectory ? "whenCreated" : LDAPConstants.CREATE_TIMESTAMP;
        String modifyTimestampLdapAttrName = activeDirectory ? "whenChanged" : LDAPConstants.MODIFY_TIMESTAMP;

        // map createTimeStamp as read-only
        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("creation date", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.CREATE_TIMESTAMP,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, createTimestampLdapAttrName,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "false");
        realm.addUserFederationMapper(mapperModel);

        // map modifyTimeStamp as read-only
        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("modify date", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.MODIFY_TIMESTAMP,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, modifyTimestampLdapAttrName,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "false");
        realm.addUserFederationMapper(mapperModel);

        // MSAD specific mapper for account state propagation
        if (activeDirectory) {
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("MSAD account controls", newProviderModel.getId(), MSADUserAccountControlMapperFactory.PROVIDER_ID);
            realm.addUserFederationMapper(mapperModel);
        }
    }


    @Override
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        syncMappers(sessionFactory, realmId, model);

        logger.infof("Sync all users from LDAP to local store: realm: %s, federation provider: %s", realmId, model.getDisplayName());

        LDAPQuery userQuery = createQuery(sessionFactory, realmId, model);
        UserFederationSyncResult syncResult = syncImpl(sessionFactory, userQuery, realmId, model);

        // TODO: Remove all existing keycloak users, which have federation links, but are not in LDAP. Perhaps don't check users, which were just added or updated during this sync?

        logger.infof("Sync all users finished: %s", syncResult.getStatus());
        return syncResult;
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        syncMappers(sessionFactory, realmId, model);

        logger.infof("Sync changed users from LDAP to local store: realm: %s, federation provider: %s, last sync time: " + lastSync, realmId, model.getDisplayName());

        // Sync newly created and updated users
        LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
        Condition createCondition = conditionsBuilder.greaterThanOrEqualTo(LDAPConstants.CREATE_TIMESTAMP, lastSync);
        Condition modifyCondition = conditionsBuilder.greaterThanOrEqualTo(LDAPConstants.MODIFY_TIMESTAMP, lastSync);
        Condition orCondition = conditionsBuilder.orCondition(createCondition, modifyCondition);

        LDAPQuery userQuery = createQuery(sessionFactory, realmId, model);
        userQuery.addWhereCondition(orCondition);
        UserFederationSyncResult result = syncImpl(sessionFactory, userQuery, realmId, model);

        logger.infof("Sync changed users finished: %s", result.getStatus());
        return result;
    }

    protected void syncMappers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                LDAPFederationProvider ldapProvider = getInstance(session, model);
                RealmModel realm = session.realms().getRealm(realmId);
                Set<UserFederationMapperModel> mappers = realm.getUserFederationMappersByFederationProvider(model.getId());
                for (UserFederationMapperModel mapperModel : mappers) {
                    UserFederationMapper ldapMapper = session.getProvider(UserFederationMapper.class, mapperModel.getFederationMapperType());
                    UserFederationSyncResult syncResult = ldapMapper.syncDataFromFederationProviderToKeycloak(mapperModel, ldapProvider, session, realm);
                    if (syncResult.getAdded() > 0 || syncResult.getUpdated() > 0 || syncResult.getRemoved() > 0 || syncResult.getFailed() > 0) {
                        logger.infof("Sync of federation mapper '%s' finished. Status: %s", mapperModel.getName(), syncResult.toString());
                    }
                }
            }

        });
    }

    protected UserFederationSyncResult syncImpl(KeycloakSessionFactory sessionFactory, LDAPQuery userQuery, final String realmId, final UserFederationProviderModel fedModel) {

        final UserFederationSyncResult syncResult = new UserFederationSyncResult();

        LDAPConfig ldapConfig = new LDAPConfig(fedModel.getConfig());
        boolean pagination = ldapConfig.isPagination();
        if (pagination) {
            int pageSize = ldapConfig.getBatchSizeForSync();

            boolean nextPage = true;
            while (nextPage) {
                userQuery.setLimit(pageSize);
                final List<LDAPObject> users = userQuery.getResultList();
                nextPage = userQuery.getPaginationContext() != null;
                UserFederationSyncResult currentPageSync = importLdapUsers(sessionFactory, realmId, fedModel, users);
                syncResult.add(currentPageSync);
            }
        } else {
            // LDAP pagination not available. Do everything in single transaction
            final List<LDAPObject> users = userQuery.getResultList();
            UserFederationSyncResult currentSync = importLdapUsers(sessionFactory, realmId, fedModel, users);
            syncResult.add(currentSync);
        }

        return syncResult;
    }

    private LDAPQuery createQuery(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        class QueryHolder {
            LDAPQuery query;
        }

        final QueryHolder queryHolder = new QueryHolder();
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                LDAPFederationProvider ldapFedProvider = getInstance(session, model);
                RealmModel realm = session.realms().getRealm(realmId);
                queryHolder.query = LDAPUtils.createQueryForUserSearch(ldapFedProvider, realm);
            }

        });
        return queryHolder.query;
    }

    protected UserFederationSyncResult importLdapUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel fedModel, List<LDAPObject> ldapUsers) {
        final UserFederationSyncResult syncResult = new UserFederationSyncResult();

        class BooleanHolder {
            private boolean value = true;
        }
        final BooleanHolder exists = new BooleanHolder();

        for (final LDAPObject ldapUser : ldapUsers) {

            try {

                // Process each user in it's own transaction to avoid global fail
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        LDAPFederationProvider ldapFedProvider = getInstance(session, fedModel);
                        RealmModel currentRealm = session.realms().getRealm(realmId);

                        String username = LDAPUtils.getUsername(ldapUser, ldapFedProvider.getLdapIdentityStore().getConfig());
                        exists.value = true;
                        LDAPUtils.checkUuid(ldapUser, ldapFedProvider.getLdapIdentityStore().getConfig());
                        UserModel currentUser = session.userStorage().getUserByUsername(username, currentRealm);

                        if (currentUser == null) {

                            // Add new user to Keycloak
                            exists.value = false;
                            ldapFedProvider.importUserFromLDAP(session, currentRealm, ldapUser);
                            syncResult.increaseAdded();

                        } else {
                            if ((fedModel.getId().equals(currentUser.getFederationLink())) && (ldapUser.getUuid().equals(currentUser.getFirstAttribute(LDAPConstants.LDAP_ID)))) {

                                // Update keycloak user
                                Set<UserFederationMapperModel> federationMappers = currentRealm.getUserFederationMappersByFederationProvider(fedModel.getId());
                                List<UserFederationMapperModel> sortedMappers = ldapFedProvider.sortMappersDesc(federationMappers);
                                for (UserFederationMapperModel mapperModel : sortedMappers) {
                                    LDAPFederationMapper ldapMapper = ldapFedProvider.getMapper(mapperModel);
                                    ldapMapper.onImportUserFromLDAP(mapperModel, ldapFedProvider, ldapUser, currentUser, currentRealm, false);
                                }

                                logger.debugf("Updated user from LDAP: %s", currentUser.getUsername());
                                syncResult.increaseUpdated();
                            } else {
                                logger.warnf("User '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s'", username, fedModel.getDisplayName());
                                syncResult.increaseFailed();
                            }
                        }
                    }

                });
            } catch (ModelException me) {
                logger.error("Failed during import user from LDAP", me);
                syncResult.increaseFailed();

                // Remove user if we already added him during this transaction
                if (!exists.value) {
                    KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                        @Override
                        public void run(KeycloakSession session) {
                            LDAPFederationProvider ldapFedProvider = getInstance(session, fedModel);
                            RealmModel currentRealm = session.realms().getRealm(realmId);
                            String username = null;
                            try {
                                username = LDAPUtils.getUsername(ldapUser, ldapFedProvider.getLdapIdentityStore().getConfig());
                            } catch (ModelException ignore) {
                            }

                            if (username != null) {
                                UserModel existing = session.userStorage().getUserByUsername(username, currentRealm);
                                if (existing != null) {
                                    session.userStorage().removeUser(currentRealm, existing);
                                }
                            }
                        }

                    });
                }
            }
        }

        return syncResult;
    }

    protected SPNEGOAuthenticator createSPNEGOAuthenticator(String spnegoToken, CommonKerberosConfig kerberosConfig) {
        KerberosServerSubjectAuthenticator kerberosAuth = createKerberosSubjectAuthenticator(kerberosConfig);
        return new SPNEGOAuthenticator(kerberosConfig, kerberosAuth, spnegoToken);
    }

    protected KerberosServerSubjectAuthenticator createKerberosSubjectAuthenticator(CommonKerberosConfig kerberosConfig) {
        return new KerberosServerSubjectAuthenticator(kerberosConfig);
    }

    protected KerberosUsernamePasswordAuthenticator createKerberosUsernamePasswordAuthenticator(CommonKerberosConfig kerberosConfig) {
        return new KerberosUsernamePasswordAuthenticator(kerberosConfig);
    }
}
