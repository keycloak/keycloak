package org.keycloak.federation.ldap;

import org.keycloak.models.utils.reflection.Reflections;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.config.LDAPMappingConfiguration;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.ldap.internal.LDAPIdentityStore;
import org.picketlink.idm.ldap.internal.LDAPOperationManager;
import org.picketlink.idm.ldap.internal.LDAPPlainTextPasswordCredentialHandler;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.spi.IdentityContext;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import static org.picketlink.idm.IDMLog.CREDENTIAL_LOGGER;
import static org.picketlink.idm.model.basic.BasicModel.getUser;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPKeycloakCredentialHandler extends LDAPPlainTextPasswordCredentialHandler {

    // Used just in ActiveDirectory to put account into "enabled" state (aka userAccountControl=512, see http://support.microsoft.com/kb/305144/en ) after password update. If value is -1, it's ignored
    private String userAccountControlAfterPasswordUpdate;

    @Override
    public void setup(LDAPIdentityStore store) {
        // TODO: Don't setup it here once PLINK-508 is fixed
        if (store.getConfig().isActiveDirectory() || Boolean.getBoolean("keycloak.ldap.ad.skipUserAccountControlAfterPasswordUpdate")) {
            String userAccountControlProp = System.getProperty("keycloak.ldap.ad.userAccountControlAfterPasswordUpdate");
            this.userAccountControlAfterPasswordUpdate = userAccountControlProp!=null ? userAccountControlProp : "512";
            CREDENTIAL_LOGGER.info("Will use userAccountControl=" + userAccountControlAfterPasswordUpdate + " after password update of user in Active Directory");
        }
    }

    // Overridden as in Keycloak, we don't have Agents
    @Override
    protected User getAccount(IdentityContext context, String loginName) {
        IdentityManager identityManager = getIdentityManager(context);

        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Trying to find account [%s] using default account type [%s]", loginName, User.class);
        }

        return getUser(identityManager, loginName);
    }

    @Override
    public void update(IdentityContext context, Account account, Password password, LDAPIdentityStore store, Date effectiveDate, Date expiryDate) {
        if (!store.getConfig().isActiveDirectory()) {
            super.update(context, account, password, store, effectiveDate, expiryDate);
        } else {
            User user = (User)account;
            LDAPOperationManager operationManager = Reflections.invokeMethod(false, KeycloakLDAPIdentityStore.GET_OPERATION_MANAGER_METHOD, LDAPOperationManager.class, store);
            IdentityManager identityManager = getIdentityManager(context);
            String userDN = getDNOfUser(user, identityManager, store, operationManager);

            updateADPassword(userDN, new String(password.getValue()), operationManager);

            if (userAccountControlAfterPasswordUpdate != null) {
                ModificationItem[] mods = new ModificationItem[1];
                BasicAttribute mod0 = new BasicAttribute("userAccountControl", userAccountControlAfterPasswordUpdate);
                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);
                operationManager.modifyAttribute(userDN, mod0);
            }
        }
    }

    protected void updateADPassword(String userDN, String password, LDAPOperationManager operationManager) {
        try {
            // Replace the "unicdodePwd" attribute with a new value
            // Password must be both Unicode and a quoted string
            String newQuotedPassword = "\"" + password + "\"";
            byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");
            BasicAttribute unicodePwd = new BasicAttribute("unicodePwd", newUnicodePassword);
            operationManager.modifyAttribute(userDN, unicodePwd);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validate(IdentityContext context, UsernamePasswordCredentials credentials,
                         LDAPIdentityStore store) {
        credentials.setStatus(Credentials.Status.INVALID);
        credentials.setValidatedAccount(null);

        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Validating credentials [%s][%s] using identity store [%s] and credential handler [%s].", credentials.getClass(), credentials, store, this);
        }

        User account = getAccount(context, credentials.getUsername());

        // If the user for the provided username cannot be found we fail validation
        if (account != null) {
            if (CREDENTIAL_LOGGER.isDebugEnabled()) {
                CREDENTIAL_LOGGER.debugf("Found account [%s] from credentials [%s].", account, credentials);
            }

            if (account.isEnabled()) {
                if (CREDENTIAL_LOGGER.isDebugEnabled()) {
                    CREDENTIAL_LOGGER.debugf("Account [%s] is ENABLED.", account, credentials);
                }

                char[] password = credentials.getPassword().getValue();

                // String bindingDN = store.getBindingDN(account);

                LDAPOperationManager operationManager = Reflections.invokeMethod(false, KeycloakLDAPIdentityStore.GET_OPERATION_MANAGER_METHOD, LDAPOperationManager.class, store);
                String bindingDN = getDNOfUser(account, getIdentityManager(context), store, operationManager);

                if (operationManager.authenticate(bindingDN, new String(password))) {
                    credentials.setValidatedAccount(account);
                    credentials.setStatus(Credentials.Status.VALID);
                }
            } else {
                if (CREDENTIAL_LOGGER.isDebugEnabled()) {
                    CREDENTIAL_LOGGER.debugf("Account [%s] is DISABLED.", account, credentials);
                }
                credentials.setStatus(Credentials.Status.ACCOUNT_DISABLED);
            }
        } else {
            if (CREDENTIAL_LOGGER.isDebugEnabled()) {
                CREDENTIAL_LOGGER.debugf("Account NOT FOUND for credentials [%s][%s].", credentials.getClass(), credentials);
            }
        }

        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Credential [%s][%s] validated using identity store [%s] and credential handler [%s]. Status [%s]. Validated Account [%s]",
                    credentials.getClass(), credentials, store, this, credentials.getStatus(), credentials.getValidatedAccount());
        }
    }

    // TODO: remove later... It's needed just because LDAPIdentityStore.getBindingDN, which always uses idProperty as first part of DN, but in AD it doesn't work as we may have idProperty 'sAMAccountName'
    // but DN like: cn=John Doe,OU=foo,DC=bar
    protected String getDNOfUser(User user, IdentityManager identityManager, LDAPIdentityStore ldapStore, LDAPOperationManager operationManager) {

        LDAPMappingConfiguration ldapEntryConfig = ldapStore.getConfig().getMappingConfig(User.class);
        IdentityQuery<User> identityQuery = identityManager.createIdentityQuery(User.class)
                .setParameter(User.LOGIN_NAME, user.getLoginName());
        StringBuilder filter = Reflections.invokeMethod(false, KeycloakLDAPIdentityStore.CREATE_SEARCH_FILTER_METHOD, StringBuilder.class, ldapStore, identityQuery, ldapEntryConfig);

        List<SearchResult> search = null;
        try {
            search = operationManager.search(ldapEntryConfig.getBaseDN(), filter.toString(), ldapEntryConfig);
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }

        if (search.size() > 0) {
            SearchResult sr1 = search.get(0);
            return sr1.getNameInNamespace();
        } else {
            return null;
        }
    }
}
