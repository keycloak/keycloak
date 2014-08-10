package org.keycloak.picketlink.idm;

import org.jboss.logging.Logger;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.event.CredentialUpdatedEvent;
import org.picketlink.idm.event.EventBridge;
import org.picketlink.idm.ldap.internal.LDAPIdentityStore;
import org.picketlink.idm.ldap.internal.LDAPOperationManager;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.spi.CredentialStore;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.StoreSelector;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakEventBridge implements EventBridge {

    private static final Logger logger = Logger.getLogger(KeycloakEventBridge.class);

    private final boolean updateUserAccountAfterPasswordUpdate;

    public KeycloakEventBridge(boolean updateUserAccountAfterPasswordUpdate) {
        this.updateUserAccountAfterPasswordUpdate = updateUserAccountAfterPasswordUpdate;
        if (updateUserAccountAfterPasswordUpdate) {
            logger.info("userAccountControl attribute will be updated in Active Directory after user registration");
        }
    }

    @Override
    public void raiseEvent(Object event) {
        // Used in ActiveDirectory to put account into "enabled" state (aka userAccountControl=512, see http://support.microsoft.com/kb/305144/en ) after password update. If value is -1, it's ignored
        if (updateUserAccountAfterPasswordUpdate && event instanceof CredentialUpdatedEvent) {
            CredentialUpdatedEvent credEvent = ((CredentialUpdatedEvent) event);
            PartitionManager partitionManager = credEvent.getPartitionMananger();
            IdentityContext identityCtx = (IdentityContext)partitionManager.createIdentityManager();

            CredentialStore store = ((StoreSelector)partitionManager).getStoreForCredentialOperation(identityCtx, credEvent.getCredential().getClass());
            if (store instanceof LDAPIdentityStore) {
                LDAPIdentityStore ldapStore = (LDAPIdentityStore)store;
                LDAPOperationManager operationManager = ldapStore.getOperationManager();
                User picketlinkUser = (User) credEvent.getAccount();
                String userDN = ldapStore.getBindingDN(picketlinkUser, true);

                ModificationItem[] mods = new ModificationItem[1];
                BasicAttribute mod0 = new BasicAttribute("userAccountControl", "512");
                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);
                operationManager.modifyAttribute(userDN, mod0);
                logger.debug("Attribute userAccountControls switched to 512 after password update of user " + picketlinkUser.getLoginName());
            } else {
                logger.debug("Store for credential updates is not LDAPIdentityStore. Ignored");
            }

        }
    }
}
