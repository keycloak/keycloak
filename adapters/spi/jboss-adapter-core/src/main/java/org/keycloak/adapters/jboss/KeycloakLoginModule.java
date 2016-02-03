/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.jboss;

import org.jboss.logging.Logger;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.spi.AbstractServerLoginModule;
import org.keycloak.adapters.spi.KeycloakAccount;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakLoginModule extends AbstractServerLoginModule {
    protected static Logger log = Logger.getLogger(KeycloakLoginModule.class);
    protected Set<String> roleSet;
    protected Principal identity;


    @SuppressWarnings("unchecked")
    @Override
    public boolean login() throws LoginException {
        log.debug("KeycloakLoginModule.login()");
        if (super.login()) {
            log.debug("super.login()==true");
            return true;
        }

        Object credential = getCredential();
        if (credential != null && (credential instanceof KeycloakAccount)) {
            log.debug("Found Account");
            KeycloakAccount account = (KeycloakAccount)credential;
            roleSet = account.getRoles();
            identity = account.getPrincipal();
            sharedState.put("javax.security.auth.login.name", identity);
            sharedState.put("javax.security.auth.login.password", credential);
            loginOk = true;
            return true;
        }

        // We return false to allow the next module to attempt authentication, maybe a
        // username and password has been supplied to a web auth.
        return false;
    }


    @Override
    protected Principal getIdentity() {
        return identity;
    }

    /*
    @Override
    protected Group[] getRoleSets() throws LoginException {
        return new Group[0];
    }
    */

    @Override
    protected Group[] getRoleSets() throws LoginException {
        //log.info("getRoleSets");
        SimpleGroup roles = new SimpleGroup("Roles");
        Group[] roleSets = {roles};
        for (String role : roleSet) {
            //log.info("   adding role: " + role);
            roles.addMember(new SimplePrincipal(role));
        }
        return roleSets;
    }

    protected Object getCredential() throws LoginException {
        NameCallback nc = new NameCallback("Alias: ");
        ObjectCallback oc = new ObjectCallback("Credential: ");
        Callback[] callbacks = { nc, oc };

        try {
            callbackHandler.handle(callbacks);

            return oc.getCredential();
        } catch (IOException ioe) {
            LoginException le = new LoginException();
            le.initCause(ioe);
            throw le;
        } catch (UnsupportedCallbackException uce) {
            LoginException le = new LoginException();
            le.initCause(uce);
            throw le;
        }
    }

}
