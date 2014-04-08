package org.keycloak.picketlink.impl;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.ldap.internal.LDAPPlainTextPasswordCredentialHandler;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.spi.IdentityContext;

import static org.picketlink.idm.IDMLog.CREDENTIAL_LOGGER;
import static org.picketlink.idm.model.basic.BasicModel.getUser;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPAgentIgnoreCredentialHandler extends LDAPPlainTextPasswordCredentialHandler {

    @Override
    protected Account getAccount(IdentityContext context, String loginName) {
        IdentityManager identityManager = getIdentityManager(context);

        if (CREDENTIAL_LOGGER.isDebugEnabled()) {
            CREDENTIAL_LOGGER.debugf("Trying to find account [%s] using default account type [%s]", loginName, User.class);
        }

        return getUser(identityManager, loginName);
    }
}
