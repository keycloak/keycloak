package org.keycloak.federation.ldap.mappers;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractTxAwareLDAPUserModelDelegate extends UserModelDelegate {

    public static final Logger logger = Logger.getLogger(AbstractTxAwareLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;
    protected LDAPObject ldapObject;
    private final LDAPTransaction transaction;

    public AbstractTxAwareLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider, LDAPObject ldapObject) {
        super(delegate);
        this.provider = provider;
        this.ldapObject = ldapObject;
        this.transaction = findOrCreateTransaction();
    }

    public LDAPTransaction getTransaction() {
        return transaction;
    }

    // Try to find transaction in any delegate. We want to enlist just single transaction per all delegates
    protected LDAPTransaction findOrCreateTransaction() {
        UserModelDelegate delegate = this;
        while (true) {
            UserModel deleg = delegate.getDelegate();
            if (!(deleg instanceof UserModelDelegate)) {
                // Existing transaction not available. Need to create new
                return new LDAPTransaction();
            } else {
                delegate = (UserModelDelegate) deleg;
            }

            // Check if it's transaction aware delegate
            if (delegate instanceof AbstractTxAwareLDAPUserModelDelegate) {
                AbstractTxAwareLDAPUserModelDelegate txDelegate = (AbstractTxAwareLDAPUserModelDelegate) delegate;
                return txDelegate.getTransaction();
            }
        }
    }

    protected void ensureTransactionStarted() {
        if (transaction.state == TransactionState.NOT_STARTED) {
            if (logger.isTraceEnabled()) {
                logger.trace("Starting and enlisting transaction for object " + ldapObject.getDn().toString());
            }

            this.provider.getSession().getTransaction().enlistAfterCompletion(transaction);
        }
    }



    protected class LDAPTransaction implements KeycloakTransaction {

        protected TransactionState state = TransactionState.NOT_STARTED;

        @Override
        public void begin() {
            if (state != TransactionState.NOT_STARTED) {
                throw new IllegalStateException("Transaction already started");
            }

            state = TransactionState.STARTED;
        }

        @Override
        public void commit() {
            if (state != TransactionState.STARTED) {
                throw new IllegalStateException("Transaction in illegal state for commit: " + state);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Transaction commit! Updating LDAP attributes for object " + ldapObject.getDn().toString() + ", attributes: " + ldapObject.getAttributes());
            }

            provider.getLdapIdentityStore().update(ldapObject);
            state = TransactionState.FINISHED;
        }

        @Override
        public void rollback() {
            if (state != TransactionState.STARTED && state != TransactionState.ROLLBACK_ONLY) {
                throw new IllegalStateException("Transaction in illegal state for rollback: " + state);
            }

            logger.warn("Transaction rollback! Ignoring LDAP updates for object " + ldapObject.getDn().toString());
            state = TransactionState.FINISHED;
        }

        @Override
        public void setRollbackOnly() {
            state = TransactionState.ROLLBACK_ONLY;
        }

        @Override
        public boolean getRollbackOnly() {
            return state == TransactionState.ROLLBACK_ONLY;
        }

        @Override
        public boolean isActive() {
            return state == TransactionState.STARTED || state == TransactionState.ROLLBACK_ONLY;
        }
    }

    protected enum TransactionState {
        NOT_STARTED, STARTED, ROLLBACK_ONLY, FINISHED
    }

}
