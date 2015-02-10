package org.keycloak.broker.kerberos.impl;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.logging.Logger;
import org.keycloak.broker.kerberos.KerberosIdentityProviderConfig;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosServerSubjectAuthenticator {

    private static final Logger logger = Logger.getLogger(KerberosServerSubjectAuthenticator.class);

    private final KerberosIdentityProviderConfig config;
    private LoginContext loginContext;

    public KerberosServerSubjectAuthenticator(KerberosIdentityProviderConfig config) {
        this.config = config;
    }

    public Subject authenticateServerSubject() throws LoginException {
        Configuration config = createJaasConfiguration();
        loginContext = new LoginContext("does-not-matter", null, null, config);
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
        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<String, Object>();
                options.put("storeKey", "true");
                options.put("doNotPrompt", "true");
                options.put("isInitiator", "false");
                options.put("useKeyTab", "true");

                options.put("keyTab", config.getKeyTab());
                options.put("principal", config.getServerPrincipal());
                options.put("debug", String.valueOf(config.getDebug()));
                AppConfigurationEntry kerberosLMConfiguration = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { kerberosLMConfiguration };
            }
        };
    }

}
