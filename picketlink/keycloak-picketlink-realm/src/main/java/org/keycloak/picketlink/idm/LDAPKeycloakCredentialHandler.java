package org.keycloak.picketlink.idm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.ldap.internal.LDAPIdentityStore;
import org.picketlink.idm.ldap.internal.LDAPOperationManager;
import org.picketlink.idm.ldap.internal.LDAPPlainTextPasswordCredentialHandler;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.spi.IdentityContext;

import static org.picketlink.idm.IDMLog.CREDENTIAL_LOGGER;
import static org.picketlink.idm.model.basic.BasicModel.getUser;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPKeycloakCredentialHandler extends LDAPPlainTextPasswordCredentialHandler {

    private static Method GET_BINDING_DN_METHOD;
    private static Method GET_OPERATION_MANAGER_METHOD;

    static {
        GET_BINDING_DN_METHOD = getMethodOnLDAPStore("getBindingDN", AttributedType.class);
        GET_OPERATION_MANAGER_METHOD = getMethodOnLDAPStore("getOperationManager");
    }

    // Used just in ActiveDirectory to put account into "enabled" state (aka userAccountControl=512, see http://support.microsoft.com/kb/305144/en ) after password update. If value is -1, it's ignored
    private String userAccountControlAfterPasswordUpdate;

    @Override
    public void setup(LDAPIdentityStore store) {
        if (store.getConfig().isActiveDirectory() || Boolean.getBoolean("keycloak.ldap.ad.skipUserAccountControlAfterPasswordUpdate")) {
            String userAccountControlProp = System.getProperty("keycloak.ldap.ad.userAccountControlAfterPasswordUpdate");
            this.userAccountControlAfterPasswordUpdate = userAccountControlProp!=null ? userAccountControlProp : "512";
            CREDENTIAL_LOGGER.info("Will use userAccountControl=" + userAccountControlAfterPasswordUpdate + " after password update of user in Active Directory");
        }
    }

    // Overridden as in Keycloak, we don't have Agents
    @Override
    protected Account getAccount(IdentityContext context, String loginName) {
        IdentityManager identityManager = getIdentityManager(context);

        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Trying to find account [%s] using default account type [%s]", loginName, User.class);
        }

        return getUser(identityManager, loginName);
    }

    @Override
    public void update(IdentityContext context, Account account, Password password, LDAPIdentityStore store, Date effectiveDate, Date expiryDate) {
        super.update(context, account, password, store, effectiveDate, expiryDate);

        if (userAccountControlAfterPasswordUpdate != null) {
            ModificationItem[] mods = new ModificationItem[1];
            BasicAttribute mod0 = new BasicAttribute("userAccountControl", userAccountControlAfterPasswordUpdate);
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);

            try {
                String bindingDN = (String) GET_BINDING_DN_METHOD.invoke(store, account);
                LDAPOperationManager operationManager = (LDAPOperationManager) GET_OPERATION_MANAGER_METHOD.invoke(store);
                operationManager.modifyAttribute(bindingDN, mod0);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getTargetException() != null ? ite.getTargetException() : ite;
                throw new RuntimeException(cause);
            }
        }
    }

    // Hack as methods are protected on LDAPIdentityStore :/
    private static Method getMethodOnLDAPStore(String methodName, Class... classes) {
        try {
            Method m = LDAPIdentityStore.class.getDeclaredMethod(methodName, classes);
            m.setAccessible(true);
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
