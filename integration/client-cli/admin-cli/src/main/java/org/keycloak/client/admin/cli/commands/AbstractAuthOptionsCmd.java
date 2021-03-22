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
package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.config.ConfigHandler;
import org.keycloak.client.admin.cli.config.FileConfigHandler;
import org.keycloak.client.admin.cli.config.InMemoryConfigHandler;
import org.keycloak.client.admin.cli.config.RealmConfigData;
import org.keycloak.client.admin.cli.util.ConfigUtil;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.IoUtil;

import java.io.File;

import static org.keycloak.client.admin.cli.config.FileConfigHandler.setConfigFile;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CLIENT;
import static org.keycloak.client.admin.cli.util.ConfigUtil.checkAuthInfo;
import static org.keycloak.client.admin.cli.util.ConfigUtil.checkServerInfo;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAuthOptionsCmd extends AbstractGlobalOptionsCmd {

    @Option(shortName = 'a', name = "admin-root", description = "URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/auth/admin")
    String adminRestRoot;

    @Option(name = "config", description = "Path to the config file (~/.keycloak/kcadm.config by default)")
    String config;

    @Option(name = "no-config", description = "Don't use config file - no authentication info is loaded or saved", hasValue = false)
    boolean noconfig;

    @Option(name = "server", description = "Server endpoint url (e.g. 'http://localhost:8080/auth')")
    String server;

    @Option(shortName = 'r', name = "target-realm", description = "Realm to target - when it's different than the realm we authenticate against")
    String targetRealm;

    @Option(name = "realm", description = "Realm name to authenticate against")
    String realm;

    @Option(name = "client", description = "Realm name to authenticate against")
    String clientId;

    @Option(name = "user", description = "Username to login with")
    String user;

    @Option(name = "password", description = "Password to login with (prompted for if not specified and --user is used)")
    String password;

    @Option(name = "secret", description = "Secret to authenticate the client (prompted for if no --user or --keystore is specified)")
    String secret;

    @Option(name = "keystore", description = "Path to a keystore containing private key")
    String keystore;

    @Option(name = "storepass", description = "Keystore password (prompted for if not specified and --keystore is used)")
    String storePass;

    @Option(name = "keypass", description = "Key password (prompted for if not specified and --keystore is used without --storepass, \n                             otherwise defaults to keystore password)")
    String keyPass;

    @Option(name = "alias", description = "Alias of the key inside a keystore (defaults to the value of ClientId)")
    String alias;

    @Option(name = "truststore", description = "Path to a truststore")
    String trustStore;

    @Option(name = "trustpass", description = "Truststore password (prompted for if not specified and --truststore is used)")
    String trustPass;

    @Option(name = "insecure", description = "Turns off TLS validation", hasValue = false)
    boolean insecure;

    @Option(name = "token", description = "Token to use for invocations.  With this option set, every other authentication option is ignored")
    String externalToken;


    protected void initFromParent(AbstractAuthOptionsCmd parent) {

        super.initFromParent(parent);

        noconfig = parent.noconfig;
        config = parent.config;
        server = parent.server;
        realm = parent.realm;
        clientId = parent.clientId;
        user = parent.user;
        password = parent.password;
        secret = parent.secret;
        keystore = parent.keystore;
        storePass = parent.storePass;
        keyPass = parent.keyPass;
        alias = parent.alias;
        trustStore = parent.trustStore;
        trustPass = parent.trustPass;
        externalToken = parent.externalToken;
    }

    protected void applyDefaultOptionValues() {
        if (clientId == null) {
            clientId = DEFAULT_CLIENT;
        }
    }

    protected boolean noOptions() {
        return externalToken == null && server == null && realm == null && clientId == null && secret == null &&
                user == null && password == null &&
                keystore == null && storePass == null && keyPass == null && alias == null &&
                trustStore == null && trustPass == null && config == null && (args == null || args.size() == 0);
    }


    protected String getTargetRealm(ConfigData config) {
        return targetRealm != null ? targetRealm : config.getRealm();
    }

    protected void processGlobalOptions() {

        super.processGlobalOptions();

        if (config != null && noconfig) {
            throw new RuntimeException("Options --config and --no-config are mutually exclusive");
        }

        if (!noconfig) {
            setConfigFile(config != null ? config : ConfigUtil.DEFAULT_CONFIG_FILE_PATH);
            ConfigUtil.setHandler(new FileConfigHandler());
        } else {
            InMemoryConfigHandler handler = new InMemoryConfigHandler();
            ConfigData data = new ConfigData();
            initConfigData(data);
            handler.setConfigData(data);
            ConfigUtil.setHandler(handler);
        }
    }

    protected void setupTruststore(ConfigData configData, CommandInvocation invocation ) {

        if (!configData.getServerUrl().startsWith("https:")) {
            return;
        }

        String truststore = trustStore;
        if (truststore == null) {
            truststore = configData.getTruststore();
        }

        if (truststore != null) {
            String pass = trustPass;
            if (pass == null) {
                pass = configData.getTrustpass();
            }
            if (pass == null) {
                pass = IoUtil.readSecret("Enter truststore password: ", invocation);
            }

            try {
                HttpUtil.setTruststore(new File(truststore), pass);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore: " + truststore, e);
            }
        }

        if (insecure) {
            HttpUtil.setSkipCertificateValidation();
        }
    }

    protected ConfigData ensureAuthInfo(ConfigData config, CommandInvocation commandInvocation) {

        if (requiresLogin()) {
            // make sure current handler is in-memory handler
            // restore it at the end
            ConfigHandler old = ConfigUtil.getHandler();
            try {
                // make sure all defaults are initialized after this point
                applyDefaultOptionValues();

                initConfigData(config);
                ConfigUtil.setupInMemoryHandler(config);

                ConfigCredentialsCmd login = new ConfigCredentialsCmd();
                login.initFromParent(this);
                login.init(config);
                login.process(commandInvocation);

                // this must be executed before finally block which restores config handler
                return loadConfig();

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                ConfigUtil.setHandler(old);
            }

        } else {
            checkAuthInfo(config);

            // make sure all defaults are initialized after this point
            applyDefaultOptionValues();
            return loadConfig();
        }
    }

    protected boolean requiresLogin() {
        return externalToken == null && (user != null || password != null || secret != null || keystore != null
                || keyPass != null || storePass != null || alias != null);
    }

    protected ConfigData copyWithServerInfo(ConfigData config) {

        ConfigData result = config.deepcopy();

        if (server != null) {
            result.setServerUrl(server);
        }
        if (realm != null) {
            result.setRealm(realm);
        }
        if (externalToken != null) {
            result.setExternalToken(externalToken);
        }

        checkServerInfo(result);
        return result;
    }

    private void initConfigData(ConfigData data) {
        if (server != null)
            data.setServerUrl(server);
        if (realm != null)
            data.setRealm(realm);
        if (trustStore != null)
            data.setTruststore(trustStore);
        if (externalToken != null) {
            data.setExternalToken(externalToken);
        }

        RealmConfigData rdata = data.sessionRealmConfigData();
        if (clientId != null)
            rdata.setClientId(clientId);
        if (secret != null)
            rdata.setSecret(secret);
    }

    protected void checkUnsupportedOptions(String ... options) {
        if (options.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of argument required");
        }

        for (int i = 0; i < options.length; i++) {
            String name = options[i];
            String value = options[++i];

            if (value != null) {
                throw new IllegalArgumentException("Unsupported option: " + name);
            }
        }
    }

    protected static String booleanOptionForCheck(boolean value) {
        return value ? "true" : null;
    }
}
