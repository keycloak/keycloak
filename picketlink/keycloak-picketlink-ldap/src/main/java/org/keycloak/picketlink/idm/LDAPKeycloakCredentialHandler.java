package org.keycloak.picketlink.idm;

import javax.naming.directory.SearchResult;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.config.LDAPMappingConfiguration;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.credential.storage.CredentialStorage;
import org.picketlink.idm.ldap.internal.LDAPIdentityStore;
import org.picketlink.idm.ldap.internal.LDAPOperationManager;
import org.picketlink.idm.ldap.internal.LDAPPlainTextPasswordCredentialHandler;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.spi.IdentityContext;

import static org.picketlink.idm.IDMLog.CREDENTIAL_LOGGER;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPKeycloakCredentialHandler extends LDAPPlainTextPasswordCredentialHandler {

    // Overridden as in Keycloak, we don't have Agents
    @Override
    protected User getAccount(IdentityContext context, String loginName) {
        IdentityManager identityManager = getIdentityManager(context);

        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Trying to find account [%s] using default account type [%s]", loginName, User.class);
        }

        return BasicModel.getUser(identityManager, loginName);
    }


    @Override
    protected boolean validateCredential(IdentityContext context, CredentialStorage credentialStorage, UsernamePasswordCredentials credentials, LDAPIdentityStore ldapIdentityStore) {
        Account account = getAccount(context, credentials.getUsername());
        char[] password = credentials.getPassword().getValue();
        String userDN = getDNOfUser(ldapIdentityStore, account);
        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Using DN [%s] for authentication of user [%s]", userDN, credentials.getUsername());
        }

        if (ldapIdentityStore.getOperationManager().authenticate(userDN, new String(password))) {
            return true;
        }

        return false;
    }

    protected String getDNOfUser(LDAPIdentityStore ldapIdentityStore, Account user) {
        LDAPMappingConfiguration userMappingConfig = ldapIdentityStore.getConfig().getMappingConfig(User.class);
        SearchResult sr = ldapIdentityStore.getOperationManager().lookupById(userMappingConfig.getBaseDN(), user.getId(), userMappingConfig);

        if (sr != null) {
            return sr.getNameInNamespace();
        } else {
            // Fallback
            return ldapIdentityStore.getBindingDN(user, true);
        }
    }
}
