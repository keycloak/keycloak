package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.picketlink.PartitionManagerProvider;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.AttributeParameter;
import org.picketlink.idm.query.Condition;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.IdentityQueryBuilder;
import org.picketlink.idm.query.QueryParameter;

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

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        throw new IllegalAccessError("Illegal to call this method");
    }

    @Override
    public LDAPFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        PartitionManagerProvider idmProvider = session.getProvider(PartitionManagerProvider.class);
        PartitionManager partition = idmProvider.getPartitionManager(model);
        return new LDAPFederationProvider(session, model, partition);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {

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
    public void syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model) {
        logger.infof("Sync all users from LDAP to local store: realm: %s, federation provider: %s, current time: " + new Date(), realmId, model.getDisplayName());

        PartitionManagerProvider idmProvider = sessionFactory.create().getProvider(PartitionManagerProvider.class);
        PartitionManager partitionMgr = idmProvider.getPartitionManager(model);
        IdentityQuery<User> userQuery = partitionMgr.createIdentityManager().createIdentityQuery(User.class);
        syncImpl(sessionFactory, userQuery, realmId, model);

        // TODO: Remove all existing keycloak users, which have federation links, but are not in LDAP. Perhaps don't check users, which were just added or updated during this sync?
    }

    @Override
    public void syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        logger.infof("Sync changed users from LDAP to local store: realm: %s, federation provider: %s, current time: " + new Date() + ", last sync time: " + lastSync, realmId, model.getDisplayName());

        PartitionManagerProvider idmProvider = sessionFactory.create().getProvider(PartitionManagerProvider.class);
        PartitionManager partitionMgr = idmProvider.getPartitionManager(model);

        // Sync newly created users
        IdentityManager identityManager = partitionMgr.createIdentityManager();
        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
        Condition condition = queryBuilder.greaterThanOrEqualTo(IdentityType.CREATED_DATE, lastSync);
        IdentityQuery<User> userQuery = queryBuilder.createIdentityQuery(User.class).where(condition);
        syncImpl(sessionFactory, userQuery, realmId, model);

        // Sync updated users
        queryBuilder = identityManager.getQueryBuilder();
        condition = queryBuilder.greaterThanOrEqualTo(LDAPUtils.MODIFY_DATE, lastSync);
        userQuery = queryBuilder.createIdentityQuery(User.class).where(condition);
        syncImpl(sessionFactory, userQuery, realmId, model);
    }

    protected void syncImpl(KeycloakSessionFactory sessionFactory, IdentityQuery<User> userQuery, final String realmId, final UserFederationProviderModel fedModel) {
        boolean pagination = Boolean.parseBoolean(fedModel.getConfig().get(LDAPConstants.PAGINATION));

        if (pagination) {
            String pageSizeConfig = fedModel.getConfig().get(LDAPConstants.BATCH_SIZE_FOR_SYNC);
            int pageSize = pageSizeConfig!=null ? Integer.parseInt(pageSizeConfig) : LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC;
            boolean nextPage = true;
            while (nextPage) {
                userQuery.setLimit(pageSize);
                final List<User> users = userQuery.getResultList();
                nextPage = userQuery.getPaginationContext() != null;

                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        importPicketlinkUsers(session, realmId, fedModel, users);
                    }

                });
            }
        } else {
            // LDAP pagination not available. Do everything in single transaction
            final List<User> users = userQuery.getResultList();
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession session) {
                    importPicketlinkUsers(session, realmId, fedModel, users);
                }

            });
        }
    }

    protected void importPicketlinkUsers(KeycloakSession session, String realmId, UserFederationProviderModel fedModel, List<User> users) {
        RealmModel realm = session.realms().getRealm(realmId);
        LDAPFederationProvider ldapFedProvider = getInstance(session, fedModel);
        ldapFedProvider.importPicketlinkUsers(realm, users, fedModel);
    }
}
