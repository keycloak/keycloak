package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.impl.KerberosServerSubjectAuthenticator;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.internal.LDAPIdentityQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapperFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationEventAwareProviderFactory;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
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
public class LDAPFederationProviderFactory extends UserFederationEventAwareProviderFactory {
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
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false");
        realm.addUserFederationMapper(mapperModel);

        // CN is typically used as RDN for Active Directory deployments
        if (ldapConfig.getRdnLdapAttribute().equalsIgnoreCase(LDAPConstants.CN)) {

            if (usernameLdapAttribute.equalsIgnoreCase(LDAPConstants.CN)) {

                // For AD deployments with "cn" as username, we will map "givenName" to first name
                mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                        UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                        UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                        UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                        UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
                realm.addUserFederationMapper(mapperModel);

            } else {
                if (editMode == UserFederationProvider.EditMode.WRITABLE) {

                    // For AD deployments with "sAMAccountName" as username and writable, we need to map "cn" as username as well (this is needed so we can register new users from KC into LDAP) and we will map "givenName" to first name.
                    mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                            UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                            UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                            UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                            UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
                    realm.addUserFederationMapper(mapperModel);

                    mapperModel = KeycloakModelUtils.createUserFederationMapperModel("username-cn", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                            UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                            UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                            UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                            UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false");
                    realm.addUserFederationMapper(mapperModel);
                } else {

                    // For read-only LDAP, we map "cn" as full name
                    mapperModel = KeycloakModelUtils.createUserFederationMapperModel("full name", newProviderModel.getId(), FullNameLDAPFederationMapperFactory.PROVIDER_ID,
                            FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                            UserAttributeLDAPFederationMapper.READ_ONLY, readOnly);
                    realm.addUserFederationMapper(mapperModel);
                }
            }
        } else {
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                    UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                    UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                    UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                    UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
            realm.addUserFederationMapper(mapperModel);
        }

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("last name", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.LAST_NAME,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.SN,
                UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
        realm.addUserFederationMapper(mapperModel);

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("email", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.EMAIL,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.EMAIL,
                UserAttributeLDAPFederationMapper.READ_ONLY, readOnly,
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
        realm.addUserFederationMapper(mapperModel);

        String createTimestampLdapAttrName = activeDirectory ? "whenCreated" : LDAPConstants.CREATE_TIMESTAMP;
        String modifyTimestampLdapAttrName = activeDirectory ? "whenChanged" : LDAPConstants.MODIFY_TIMESTAMP;

        // map createTimeStamp as read-only
        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("creation date", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.CREATE_TIMESTAMP,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, createTimestampLdapAttrName,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
        realm.addUserFederationMapper(mapperModel);

        // map modifyTimeStamp as read-only
        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("modify date", newProviderModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.MODIFY_TIMESTAMP,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, modifyTimestampLdapAttrName,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP);
        realm.addUserFederationMapper(mapperModel);
    }


    @Override
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        logger.infof("Sync all users from LDAP to local store: realm: %s, federation provider: %s", realmId, model.getDisplayName());

        LDAPIdentityQuery userQuery = createQuery(sessionFactory, realmId, model);
        UserFederationSyncResult syncResult = syncImpl(sessionFactory, userQuery, realmId, model);

        // TODO: Remove all existing keycloak users, which have federation links, but are not in LDAP. Perhaps don't check users, which were just added or updated during this sync?

        logger.infof("Sync all users finished: %s", syncResult.getStatus());
        return syncResult;
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        logger.infof("Sync changed users from LDAP to local store: realm: %s, federation provider: %s, last sync time: " + lastSync, realmId, model.getDisplayName());

        // Sync newly created and updated users
        LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
        Condition createCondition = conditionsBuilder.greaterThanOrEqualTo(new QueryParameter(LDAPConstants.CREATE_TIMESTAMP), lastSync);
        Condition modifyCondition = conditionsBuilder.greaterThanOrEqualTo(new QueryParameter(LDAPConstants.MODIFY_TIMESTAMP), lastSync);
        Condition orCondition = conditionsBuilder.orCondition(createCondition, modifyCondition);

        LDAPIdentityQuery userQuery = createQuery(sessionFactory, realmId, model);
        userQuery.where(orCondition);
        UserFederationSyncResult result = syncImpl(sessionFactory, userQuery, realmId, model);

        logger.infof("Sync changed users finished: %s", result.getStatus());
        return result;
    }

    protected UserFederationSyncResult syncImpl(KeycloakSessionFactory sessionFactory, LDAPIdentityQuery userQuery, final String realmId, final UserFederationProviderModel fedModel) {

        final UserFederationSyncResult syncResult = new UserFederationSyncResult();

        boolean pagination = Boolean.parseBoolean(fedModel.getConfig().get(LDAPConstants.PAGINATION));
        if (pagination) {

            String pageSizeConfig = fedModel.getConfig().get(LDAPConstants.BATCH_SIZE_FOR_SYNC);
            int pageSize = pageSizeConfig!=null ? Integer.parseInt(pageSizeConfig) : LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC;

            boolean nextPage = true;
            while (nextPage) {
                userQuery.setLimit(pageSize);
                final List<LDAPObject> users = userQuery.getResultList();
                nextPage = userQuery.getPaginationContext() != null;

                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        UserFederationSyncResult currentPageSync = importLdapUsers(session, realmId, fedModel, users);
                        syncResult.add(currentPageSync);
                    }

                });
            }
        } else {
            // LDAP pagination not available. Do everything in single transaction
            final List<LDAPObject> users = userQuery.getResultList();
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession session) {
                    UserFederationSyncResult currentSync = importLdapUsers(session, realmId, fedModel, users);
                    syncResult.add(currentSync);
                }

            });
        }

        return syncResult;
    }

    private LDAPIdentityQuery createQuery(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        class QueryHolder {
            LDAPIdentityQuery query;
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


    protected UserFederationSyncResult importLdapUsers(KeycloakSession session, String realmId, UserFederationProviderModel fedModel, List<LDAPObject> ldapUsers) {
        RealmModel realm = session.realms().getRealm(realmId);
        LDAPFederationProvider ldapFedProvider = getInstance(session, fedModel);
        return ldapFedProvider.importLDAPUsers(realm, ldapUsers, fedModel);
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
