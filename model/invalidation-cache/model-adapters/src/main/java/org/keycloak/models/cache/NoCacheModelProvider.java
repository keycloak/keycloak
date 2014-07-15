package org.keycloak.models.cache;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class NoCacheModelProvider implements CacheModelProvider {
    protected KeycloakSession session;
    protected ModelProvider delegate;
//    protected KeycloakTransaction transactionDelegate;
//    protected boolean transactionActive;
//    protected boolean setRollbackOnly;

    public NoCacheModelProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ModelProvider getDelegate() {
//        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.getProvider(ModelProvider.class);
//        transactionDelegate = delegate.getTransaction();
//        if (!transactionDelegate.isActive()) {
//            transactionDelegate.begin();
//            if (setRollbackOnly) {
//                transactionDelegate.setRollbackOnly();
//            }
//        }
        return delegate;
    }

    @Override
    public void registerRealmInvalidation(String id) {
    }

    @Override
    public void registerApplicationInvalidation(String id) {
    }

    @Override
    public void registerRoleInvalidation(String id) {
    }

    @Override
    public void registerOAuthClientInvalidation(String id) {
    }

    @Override
    public RealmModel createRealm(String name) {
        return getDelegate().createRealm(name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        return getDelegate().createRealm(id, name);
    }

    @Override
    public RealmModel getRealm(String id) {
        return getDelegate().getRealm(id);
    }

    @Override
    public RealmModel getRealmByName(String name) {
        return getDelegate().getRealmByName(name);
    }

    @Override
    public List<RealmModel> getRealms() {
        // we don't cache this for now
        return getDelegate().getRealms();
    }

    @Override
    public boolean removeRealm(String id) {
        return getDelegate().removeRealm(id);
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
       return getDelegate().getRoleById(id, realm);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return getDelegate().getApplicationById(id, realm);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return getDelegate().getOAuthClientById(id, realm);
    }

    @Override
    public void registerUserInvalidation(String id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
