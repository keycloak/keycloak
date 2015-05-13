package org.keycloak.federation.ldap.mappers;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TxAwareLDAPUserModelDelegate extends UserModelDelegate {

    private static final Logger logger = Logger.getLogger(TxAwareLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;
    protected LDAPObject ldapObject;
    protected LDAPTransaction transaction;

    // Map of allowed writable UserModel attributes to LDAP attributes. Includes UserModel properties (firstName, lastName, email, ...)
    private final Map<String, String> mappedAttributes = new HashMap<String, String>();

    public TxAwareLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider, LDAPObject ldapObject) {
        super(delegate);
        this.provider = provider;
        this.ldapObject = ldapObject;
    }

    public void addMappedAttribute(String userModelAttrName, String ldapAttrName) {
        mappedAttributes.put(userModelAttrName, ldapAttrName);
    }

    @Override
    public void setAttribute(String name, String value) {
        setLDAPAttribute(name, value);

        super.setAttribute(name, value);
    }

    @Override
    public void setEmail(String email) {
        setLDAPAttribute(UserModel.EMAIL, email);

        super.setEmail(email);
    }

    @Override
    public void setLastName(String lastName) {
        setLDAPAttribute(UserModel.LAST_NAME, lastName);

        super.setLastName(lastName);
    }

    @Override
    public void setFirstName(String firstName) {
        setLDAPAttribute(UserModel.FIRST_NAME, firstName);

        super.setFirstName(firstName);
    }

    protected void setLDAPAttribute(String modelAttrName, String value) {
        String ldapAttrName = mappedAttributes.get(modelAttrName);
        if (ldapAttrName != null) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Pushing user attribute to LDAP. Model attribute name: %s, LDAP attribute name: %s, Attribute value: %s", modelAttrName, ldapAttrName, value);
            }

            if (transaction == null) {
                transaction = new LDAPTransaction();
                provider.getSession().getTransaction().enlistAfterCompletion(transaction);
            }

            ldapObject.setAttribute(ldapAttrName, value);
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
