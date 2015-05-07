package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.impl.KerberosServerSubjectAuthenticator;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQuery;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQueryBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
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
public class LDAPFederationProviderFactory implements UserFederationProviderFactory {
    private static final Logger logger = Logger.getLogger(LDAPFederationProviderFactory.class);
    public static final String PROVIDER_NAME = "ldap";

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
    public void postInit(KeycloakSessionFactory factory) {

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

    @Override
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model) {
        logger.infof("Sync all users from LDAP to local store: realm: %s, federation provider: %s", realmId, model.getDisplayName());

        LDAPIdentityStore ldapIdentityStore = this.ldapStoreRegistry.getLdapStore(model);
        IdentityQuery<LDAPUser> userQuery = ldapIdentityStore.createQueryBuilder().createIdentityQuery(LDAPUser.class);
        UserFederationSyncResult syncResult = syncImpl(sessionFactory, userQuery, realmId, model);

        // TODO: Remove all existing keycloak users, which have federation links, but are not in LDAP. Perhaps don't check users, which were just added or updated during this sync?

        logger.infof("Sync all users finished: %s", syncResult.getStatus());
        return syncResult;
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        logger.infof("Sync changed users from LDAP to local store: realm: %s, federation provider: %s, last sync time: " + lastSync, realmId, model.getDisplayName());

        LDAPIdentityStore ldapIdentityStore = this.ldapStoreRegistry.getLdapStore(model);

        // Sync newly created and updated users
        IdentityQueryBuilder queryBuilder = ldapIdentityStore.createQueryBuilder();
        Condition createCondition = queryBuilder.greaterThanOrEqualTo(IdentityType.CREATED_DATE, lastSync);
        Condition modifyCondition = queryBuilder.greaterThanOrEqualTo(LDAPUtils.MODIFY_DATE, lastSync);
        Condition orCondition = queryBuilder.orCondition(createCondition, modifyCondition);
        IdentityQuery<LDAPUser> userQuery = queryBuilder.createIdentityQuery(LDAPUser.class).where(orCondition);
        UserFederationSyncResult result = syncImpl(sessionFactory, userQuery, realmId, model);

        logger.infof("Sync changed users finished: %s", result.getStatus());
        return result;
    }

    protected UserFederationSyncResult syncImpl(KeycloakSessionFactory sessionFactory, IdentityQuery<LDAPUser> userQuery, final String realmId, final UserFederationProviderModel fedModel) {

        final UserFederationSyncResult syncResult = new UserFederationSyncResult();

        boolean pagination = Boolean.parseBoolean(fedModel.getConfig().get(LDAPConstants.PAGINATION));
        if (pagination) {

            String pageSizeConfig = fedModel.getConfig().get(LDAPConstants.BATCH_SIZE_FOR_SYNC);
            int pageSize = pageSizeConfig!=null ? Integer.parseInt(pageSizeConfig) : LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC;

            boolean nextPage = true;
            while (nextPage) {
                userQuery.setLimit(pageSize);
                final List<LDAPUser> users = userQuery.getResultList();
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
            final List<LDAPUser> users = userQuery.getResultList();
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

    protected UserFederationSyncResult importLdapUsers(KeycloakSession session, String realmId, UserFederationProviderModel fedModel, List<LDAPUser> ldapUsers) {
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
