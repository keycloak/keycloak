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
package org.keycloak.client.cli.common;

import java.io.File;
import java.io.PrintWriter;

import org.keycloak.OAuth2Constants;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.ConfigHandler;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.config.InMemoryConfigHandler;
import org.keycloak.client.cli.config.RealmConfigData;
import org.keycloak.client.cli.util.AuthUtil;
import org.keycloak.client.cli.util.ConfigUtil;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.util.IoUtils;

import picocli.CommandLine.Option;

import static org.keycloak.client.cli.config.FileConfigHandler.setConfigFile;
import static org.keycloak.client.cli.util.ConfigUtil.DEFAULT_CLIENT;
import static org.keycloak.client.cli.util.ConfigUtil.checkServerInfo;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class BaseAuthOptionsCmd extends BaseGlobalOptionsCmd {

    public static final String DEFAULT_CONFIG_PATH_STRING_KEY = "default.config.path.string";

    @Option(names = "--config", description = "Path to the config file (${sys:"+DEFAULT_CONFIG_PATH_STRING_KEY+"} by default)")
    protected String config;

    @Option(names = "--no-config", description = "Don't use config file - no authentication info is loaded or saved")
    protected boolean noconfig;

    @Option(names = "--server", description = "Server endpoint url (e.g. 'http://localhost:8080')")
    protected String server;

    @Option(names = "--realm", description = "Realm name to authenticate against")
    protected String realm;

    @Option(names = "--client", description = "Realm name to authenticate against")
    protected String clientId;

    @Option(names = "--user", description = "Username to login with")
    protected String user;

    @Option(names = "--password", description = "Password to login with (prompted for if not specified, --user is used, and the env variable KC_CLI_PASSWORD is not defined)")
    protected String password;

    @Option(names = "--secret", description = "Secret to authenticate the client (prompted for if no --user nor --keystore is specified, and the env variable KC_CLI_CLIENT_SECRET is not defined)")
    protected String secret;

    @Option(names = "--keystore", description = "Path to a keystore containing private key")
    protected String keystore;

    @Option(names = "--storepass", description = "Keystore password (prompted for if not specified, --keystore is used, and the env variable KC_CLI_STORE_PASSWORD is undefined)")
    protected String storePass;

    @Option(names = "--keypass", description = "Key password (prompted for if not specified, --keystore is used without --storepass, and the env variable KC_CLI_KEY_PASSWORD is undefined, otherwise defaults to keystore password)")
    protected String keyPass;

    @Option(names = "--alias", description = "Alias of the key inside a keystore (defaults to the value of ClientId)")
    protected String alias;

    @Option(names = "--truststore", description = "Path to a truststore")
    protected String trustStore;

    @Option(names = "--trustpass", description = "Truststore password (prompted for if not specified, --user is used, and the env variable KC_CLI_TRUSTSTORE_PASSWORD is not defined)")
    protected String trustPass;

    @Option(names = "--insecure", description = "Turns off TLS validation")
    protected boolean insecure;

    // subclasses unfortunately use different options for this, so they must be declared elsewhere
    protected String externalToken;

    protected CommandState commandState;

    public BaseAuthOptionsCmd(CommandState state) {
        this.commandState = state;
    }

    protected String getCommand() {
        return commandState.getCommand();
    }

    protected String getDefaultConfigFilePath() {
        return commandState.getDefaultConfigFilePath();
    }

    protected void initFromParent(BaseAuthOptionsCmd parent) {
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

    @Override
    protected boolean nothingToDo() {
        return externalToken == null && server == null && realm == null && clientId == null && secret == null &&
                user == null && password == null &&
                keystore == null && storePass == null && keyPass == null && alias == null &&
                trustStore == null && trustPass == null && config == null;
    }

    @Override
    protected void processOptions() {
        if (config != null && noconfig) {
            throw new IllegalArgumentException("Options --config and --no-config are mutually exclusive");
        }

        if (!noconfig) {
            setConfigFile(config != null ? config : getDefaultConfigFilePath());
            ConfigUtil.setHandler(new FileConfigHandler());
        } else {
            InMemoryConfigHandler handler = new InMemoryConfigHandler();
            ConfigData data = new ConfigData();
            initConfigData(data);
            handler.setConfigData(data);
            ConfigUtil.setHandler(handler);
        }
    }

    protected void setupTruststore(ConfigData configData) {

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
            	pass = System.getenv("KC_CLI_TRUSTSTORE_PASSWORD");
            }
            if (pass == null) {            	
                pass = IoUtils.readPasswordFromConsole("truststore password");
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

    protected ConfigData ensureAuthInfo(ConfigData config) {

        if (requiresLogin()) {
            // make sure current handler is in-memory handler
            // restore it at the end
            ConfigHandler old = ConfigUtil.getHandler();
            try {
                // make sure all defaults are initialized after this point
                applyDefaultOptionValues();

                initConfigData(config);
                ConfigUtil.setupInMemoryHandler(config);

                BaseConfigCredentialsCmd login = new BaseConfigCredentialsCmd(commandState);
                login.initFromParent(this);
                login.init(config);
                login.process();

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
            checkServerInfo(config, getCommand());

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

        checkServerInfo(result, getCommand());
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
        String grantTypeForAuthentication = user == null ? OAuth2Constants.CLIENT_CREDENTIALS : OAuth2Constants.PASSWORD;
        rdata.setGrantTypeForAuthentication(grantTypeForAuthentication);
    }

    protected String ensureToken(ConfigData config) {
        return AuthUtil.ensureToken(config, getCommand());
    }

    protected void globalOptions(PrintWriter out) {
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println("    --config              Path to the config file (" + commandState.getDefaultConfigFilePath() + " by default)");
        out.println("    --no-config           Don't use config file - no authentication info is loaded or saved");
        if (commandState.isTokenGlobal()) {
            out.println("    --token               Token to use to invoke on Keycloak.  Other credential may be ignored if this flag is set.");
        }
        out.println("    --truststore PATH     Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD  Truststore password (prompted for if not specified, --truststore is used, and the KC_CLI_TRUSTSTORE_PASSWORD env property is not defined)");
        out.println("    CREDENTIALS OPTIONS   Same set of options as accepted by '" + commandState.getCommand() + " config credentials' in order to establish");
        out.println("                          an authenticated sessions. In combination with --no-config option this allows transient");
        out.println("                          (on-the-fly) authentication to be performed which leaves no tokens in config file.");
        out.println();
    }

}
