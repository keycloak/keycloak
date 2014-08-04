package org.keycloak.picketlink.idm;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.ldap.internal.LDAPPlainTextPasswordCredentialHandler;
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
}
