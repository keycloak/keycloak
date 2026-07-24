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

package org.keycloak.federation.kerberos.impl;

import java.io.IOException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.keycloak.common.util.KerberosJdkProvider;
import org.keycloak.federation.kerberos.CommonKerberosConfig;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosServerSubjectAuthenticator {

    private static final Logger logger = Logger.getLogger(KerberosServerSubjectAuthenticator.class);

    private static final CallbackHandler NO_CALLBACK_HANDLER = new CallbackHandler() {

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            throw new UnsupportedCallbackException(callbacks[0]);
        }
    };


    private final CommonKerberosConfig config;
    private LoginContext loginContext;


    public KerberosServerSubjectAuthenticator(CommonKerberosConfig config) {
        this.config = config;
    }


    public Subject authenticateServerSubject() throws LoginException {
        Configuration config = createJaasConfiguration();
        loginContext = new LoginContext("does-not-matter", null, NO_CALLBACK_HANDLER, config);
        loginContext.login();
        return loginContext.getSubject();
    }


    public void logoutServerSubject() {
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException le) {
                logger.error("Failed to logout kerberos server subject: " + config.getServerPrincipal(), le);
            }
        }
    }


    protected Configuration createJaasConfiguration() {
        return KerberosJdkProvider.getProvider().createJaasConfigurationForServer(config.getKeyTab(), config.getServerPrincipal(), config.isDebug());
    }

}
