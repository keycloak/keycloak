package org.keycloak.client.registration.cli.commands;

import org.keycloak.OAuth2Constants;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.ConfigHandler;
import org.keycloak.client.registration.cli.config.FileConfigHandler;
import org.keycloak.client.registration.cli.config.InMemoryConfigHandler;
import org.keycloak.client.registration.cli.config.RealmConfigData;
import org.keycloak.client.registration.cli.util.ConfigUtil;
import org.keycloak.client.registration.cli.util.HttpUtil;
import org.keycloak.client.registration.cli.util.IoUtil;

import java.io.File;

import picocli.CommandLine.Option;

import static org.keycloak.client.registration.cli.config.FileConfigHandler.setConfigFile;
import static org.keycloak.client.registration.cli.util.ConfigUtil.checkAuthInfo;
import static org.keycloak.client.registration.cli.util.ConfigUtil.checkServerInfo;
import static org.keycloak.client.registration.cli.util.ConfigUtil.loadConfig;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAuthOptionsCmd extends AbstractGlobalOptionsCmd {

    static final String DEFAULT_CLIENT = "admin-cli";

    @Option(names = "--config", description = "Path to the config file (~/.keycloak/kcreg.config by default)")
    protected String config;

    @Option(names = "--no-config", description = "No configuration file should be used, no authentication info is loaded or saved")
    protected boolean noconfig;

    @Option(names = "--server", description = "Server endpoint url (e.g. 'http://localhost:8080')")
    protected String server;

    @Option(names = "--realm", description = "Realm name to authenticate against")
    protected String realm;

    @Option(names = "--client", description = "Realm name to authenticate against")
    protected String clientId;

    @Option(names = "--user", description = "Username to login with")
    protected String user;

    @Option(names = "--password", description = "Password to login with (prompted for if not specified and --user is used)")
    protected String password;

    @Option(names = "--secret", description = "Secret to authenticate the client (prompted for if no --user or --keystore is specified)")
    protected String secret;

    @Option(names = "--keystore", description = "Path to a keystore containing private key")
    protected String keystore;

    @Option(names = "--storepass", description = "Keystore password (prompted for if not specified and --keystore is used)")
    protected String storePass;

    @Option(names = "--keypass", description = "Key password (prompted for if not specified and --keystore is used without --storepass, \n                             otherwise defaults to keystore password)")
    protected String keyPass;

    @Option(names = "--alias", description = "Alias of the key inside a keystore (defaults to the value of ClientId)")
    protected String alias;

    @Option(names = "--truststore", description = "Path to a truststore")
    protected String trustStore;

    @Option(names = "--trustpass", description = "Truststore password (prompted for if not specified and --truststore is used)")
    protected String trustPass;

    @Option(names = "--insecure", description = "Turns off TLS validation")
    protected boolean insecure;

    @Option(names = {"-t", "--token"}, description = "Initial / Registration access token to use)")
    protected String token;

    protected void initFromParent(AbstractAuthOptionsCmd parent) {
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
        token = parent.token;
        insecure = parent.insecure;
    }

    protected void applyDefaultOptionValues() {
        if (clientId == null) {
            clientId = DEFAULT_CLIENT;
        }
    }

    protected boolean noOptions() {
        return server == null && realm == null && clientId == null && secret == null &&
                user == null && password == null &&
                keystore == null && storePass == null && keyPass == null && alias == null &&
                trustStore == null && trustPass == null &&
                token == null && config == null;
    }

    @Override
    protected void processOptions() {

        if (config != null && noconfig) {
            throw new IllegalArgumentException("Options --config and --no-config are mutually exclusive");
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
                pass = IoUtil.readSecret("Enter truststore password: ");
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

                ConfigCredentialsCmd login = new ConfigCredentialsCmd();
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
            checkAuthInfo(config);

            // make sure all defaults are initialized after this point
            applyDefaultOptionValues();
            return loadConfig();
        }
    }

    protected boolean requiresLogin() {
        return user != null || password != null || secret != null || keystore != null
                || keyPass != null || storePass != null || alias != null;
    }

    protected ConfigData copyWithServerInfo(ConfigData config) {

        ConfigData result = config.deepcopy();

        if (server != null) {
            result.setServerUrl(server);
        }
        if (realm != null) {
            result.setRealm(realm);
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

        RealmConfigData rdata = data.sessionRealmConfigData();
        if (clientId != null)
            rdata.setClientId(clientId);
        if (secret != null)
            rdata.setSecret(secret);
        String grantTypeForAuthentication = user == null ? OAuth2Constants.CLIENT_CREDENTIALS : OAuth2Constants.PASSWORD;
        rdata.setGrantTypeForAuthentication(grantTypeForAuthentication);
    }

    @Override
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
