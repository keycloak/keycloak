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
package org.keycloak.adapters.installed;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwe.*;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;

/**
 * All kcinit commands that take input ask for
 * <p>
 * 1. . kcinit
 * - setup and export KC_SESSION_KEY env var if not set.
 * - checks to see if master token valid, refresh is possible, exit if token valid
 * - performs command line login
 * - stores master token for master client
 * 2. app.sh is a wrapper for app cli.
 * - token=`kcinit token app`
 * - checks to see if token for app client has been fetched, refresh if valid, output token to sys.out if exists
 * - if no token, login.  Prompts go to stderr.
 * - pass token as cmd line param to app or as environment variable.
 * <p>
 * 3. kcinit password {password}
 * - outputs password key that is used for encryption.
 * - can be used in .bashrc as export KC_SESSSION_KEY=`kcinit password {password}` or just set it in .bat file
 * <p>
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KcinitDriver {

    public static final String KC_SESSION_KEY = "KC_SESSION_KEY";
    public static final String KC_LOGIN_CONFIG_PATH = "KC_LOGIN_CONFIG_PATH";
    protected Map<String, String> config;
    protected boolean debug = true;

    protected static byte[] salt = new byte[]{-4, 88, 66, -101, 78, -94, 21, 105};

    String[] args = null;

    protected boolean forceLogin;
    protected boolean browserLogin;

    public void mainCmd(String[] args) throws Exception {

        this.args = args;


        if (args.length == 0) {
            printHelp();
            return;
        }

        if (args[0].equalsIgnoreCase("token")) {
            //System.err.println("executing token");
            token();
        } else if (args[0].equalsIgnoreCase("login")) {
            login();
        } else if (args[0].equalsIgnoreCase("logout")) {
            logout();
        } else if (args[0].equalsIgnoreCase("env")) {
            System.out.println(System.getenv().toString());
        } else if (args[0].equalsIgnoreCase("install")) {
            install();
        } else if (args[0].equalsIgnoreCase("uninstall")) {
            uninstall();
        } else if (args[0].equalsIgnoreCase("password")) {
            passwordKey();
        } else {
            KeycloakInstalled.console().writer().println("Unknown command: " + args[0]);
            KeycloakInstalled.console().writer().println();
            printHelp();
        }
    }

    public String getHome() {
        String home = System.getenv("HOME");
        if (home == null) {
            home = System.getProperty("HOME");
            if (home == null) {
                home = Paths.get("").toAbsolutePath().normalize().toString();
            }
        }
        return home;
    }

    public void passwordKey() {
        if (args.length < 2) {
            printHelp();
            System.exit(1);
        }
        String password = args[1];
        try {
            String encodedKey = generateEncryptionKey(password);
            System.out.printf(encodedKey);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected String generateEncryptionKey(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100, 128);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] aeskey = tmp.getEncoded();
        return Base64.encodeBytes(aeskey);
    }

    public JWE createJWE() {
        String key = getEncryptionKey();
        if (key == null) {
            throw new RuntimeException(KC_SESSION_KEY + " env var not set");
        }
        byte[] aesKey = null;
        try {
            aesKey = Base64.decode(key.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("invalid " + KC_SESSION_KEY + "env var");
        }

        JWE jwe = new JWE();
        final SecretKey aesSecret = new SecretKeySpec(aesKey, "AES");
        jwe.getKeyStorage()
                .setDecryptionKey(aesSecret);
        return jwe;
    }

    protected String encryptionKey;

    protected String getEncryptionKey() {
        if (encryptionKey != null) return encryptionKey;
        return System.getenv(KC_SESSION_KEY);
    }

    public String encrypt(String payload) {
        JWE jwe = createJWE();
        JWEHeader jweHeader = new JWEHeader(JWEConstants.A128KW, JWEConstants.A128CBC_HS256, null);
        jwe.header(jweHeader).content(payload.getBytes(StandardCharsets.UTF_8));
        try {
            return jwe.encodeJwe();
        } catch (JWEException e) {
            throw new RuntimeException("cannot encrypt payload", e);
        }
    }

    public String decrypt(String encoded) {
        JWE jwe = createJWE();
        try {
            jwe.verifyAndDecodeJwe(encoded);
            byte[] content = jwe.getContent();
            if (content == null) return null;
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("cannot decrypt payload", ex);

        }

    }

    public static String getenv(String name, String defaultValue) {
        String val = System.getenv(name);
        return val == null ? defaultValue : val;
    }

    public File getConfigDirectory() {
        return Paths.get(getHome(), getenv(KC_LOGIN_CONFIG_PATH, ".keycloak"), "kcinit").toFile();
    }


    public File getConfigFile() {
        return Paths.get(getHome(), getenv(KC_LOGIN_CONFIG_PATH, ".keycloak"), "kcinit", "config.json").toFile();
    }

    public File getTokenFilePath(String client) {
        return Paths.get(getHome(), getenv(KC_LOGIN_CONFIG_PATH, ".keycloak"), "kcinit", "tokens", client).toFile();
    }

    public File getTokenDirectory() {
        return Paths.get(getHome(), getenv(KC_LOGIN_CONFIG_PATH, ".keycloak"), "kcinit", "tokens").toFile();
    }

    protected boolean encrypted = false;

    protected void checkEnv() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            KeycloakInstalled.console().writer().println("You have not configured kcinit.  Please run 'kcinit install' to configure.");
            System.exit(1);
        }
        byte[] data = new byte[0];
        try {
            data = readFileRaw(configFile);
        } catch (IOException e) {

        }
        if (data == null) {
            KeycloakInstalled.console().writer().println("Config file unreadable.  Please run 'kcinit install' to configure.");
            System.exit(1);

        }
        String encodedJwe = new String(data, StandardCharsets.UTF_8);

        if (encodedJwe.contains("realm")) {
            encrypted = false;
            return;
        } else {
            encrypted = true;
        }

        if (System.getenv(KC_SESSION_KEY) == null) {
            promptLocalPassword();
        }
    }

    protected void promptLocalPassword() {
        String password = KeycloakInstalled.console().passwordPrompt("Enter password to unlock kcinit config files: ");
        try {
            encryptionKey = generateEncryptionKey(password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected String readFile(File fp) {
        try {
            byte[] data = readFileRaw(fp);
            if (data == null) return null;
            String file = new String(data, StandardCharsets.UTF_8);
            if (!encrypted) {
                return file;
            }
            String decrypted = decrypt(file);
            if (decrypted == null)
                throw new RuntimeException("Unable to decrypt file.  Did you set your local password correctly?");
            return decrypted;
        } catch (IOException e) {
            throw new RuntimeException("failed to decrypt file: " + fp.getAbsolutePath() + " Did you set your local password correctly?", e);
        }


    }

    protected byte[] readFileRaw(File fp) throws IOException {
        if (!fp.exists()) return null;
        FileInputStream fis = new FileInputStream(fp);
        byte[] data = new byte[(int) fp.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    protected void writeFile(File fp, String payload) {
        try {
            String data = payload;
            if (encrypted) data = encrypt(payload);
            FileOutputStream fos = new FileOutputStream(fp);
            fos.write(data.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void install() {
        if (getEncryptionKey() == null) {
            if (KeycloakInstalled.console().confirm("Do you want to protect tokens stored locally with a password? (y/n): ")) {
                String password = "p";
                String confirm = "c";
                do {
                    password = KeycloakInstalled.console().passwordPrompt("Enter local password: ");
                    confirm = KeycloakInstalled.console().passwordPrompt("Confirm local password: ");
                    if (!password.equals(confirm)) {
                        KeycloakInstalled.console().writer().println();
                        KeycloakInstalled.console().writer().println("Confirmation does not match.  Try again.");
                        KeycloakInstalled.console().writer().println();
                    }
                } while (!password.equals(confirm));
                try {
                    this.encrypted = true;
                    this.encryptionKey = generateEncryptionKey(password);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } else {
            if (!KeycloakInstalled.console().confirm("KC_SESSION_KEY env var already set.  Do you want to use this as your local encryption key? (y/n): ")) {
                KeycloakInstalled.console().writer().println("Unset KC_SESSION_KEY env var and run again");
                System.exit(1);
            }
            this.encrypted = true;
            this.encryptionKey = getEncryptionKey();
        }
        String server = KeycloakInstalled.console().readLine("Authentication server URL [http://localhost:8080/auth]: ").trim();
        String realm = KeycloakInstalled.console().readLine("Name of realm [master]: ").trim();
        String client = KeycloakInstalled.console().readLine("CLI client id [kcinit]: ").trim();
        String secret = KeycloakInstalled.console().readLine("CLI client secret [none]: ").trim();
        if (server.equals("")) {
            server = "http://localhost:8080/auth";
        }
        if (realm.equals("")) {
            realm = "master";
        }
        if (client.equals("")) {
            client = "kcinit";
        }
        File configDir = getTokenDirectory();
        configDir.mkdirs();

        File configFile = getConfigFile();
        Map<String, String> props = new HashMap<>();
        props.put("server", server);
        props.put("realm", realm);
        props.put("client", client);
        props.put("secret", secret);

        try {
            String json = JsonSerialization.writeValueAsString(props);
            writeFile(configFile, json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        KeycloakInstalled.console().writer().println();
        KeycloakInstalled.console().writer().println("Installation complete!");
        KeycloakInstalled.console().writer().println();
    }


    public void printHelp() {
        KeycloakInstalled.console().writer().println("Commands:");
        KeycloakInstalled.console().writer().println("  login [-f] -f forces login");
        KeycloakInstalled.console().writer().println("  logout");
        KeycloakInstalled.console().writer().println("  token [client] - print access token of desired client.  Defaults to default master client.  Will print either 'error', 'not-allowed',  or 'login-required' on error.");
        KeycloakInstalled.console().writer().println("  install - Install this utility.  Will store in $HOME/.keycloak/kcinit unless " + KC_LOGIN_CONFIG_PATH + " env var is set");
        System.exit(1);
    }


    public AdapterConfig getConfig() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            KeycloakInstalled.console().writer().println("You have not configured kcinit.  Please run 'kcinit install' to configure.");
            System.exit(1);
            return null;
        }

        AdapterConfig config = new AdapterConfig();
        config.setAuthServerUrl(getConfigProperties().get("server"));
        config.setRealm(getConfigProperties().get("realm"));
        config.setResource(getConfigProperties().get("client"));
        config.setSslRequired("external");
        String secret = getConfigProperties().get("secret");
        if (secret != null && !secret.trim().equals("")) {
            Map<String, Object> creds = new HashMap<>();
            creds.put("secret", secret);
            config.setCredentials(creds);
        } else {
            config.setPublicClient(true);
        }
        return config;
    }

    private Map<String, String> getConfigProperties() {
        if (this.config != null) return this.config;
        if (!getConfigFile().exists()) {
            KeycloakInstalled.console().writer().println();
            KeycloakInstalled.console().writer().println(("Config file does not exist.  Run kcinit install to set it up."));
            System.exit(1);
        }
        String json = readFile(getConfigFile());
        try {
            Map map = JsonSerialization.readValue(json, Map.class);
            config = (Map<String, String>) map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this.config;
    }

    public String readToken(String client) throws Exception {
        String json = getTokenResponse(client);
        if (json == null) return null;


        if (json != null) {
            try {
                AccessTokenResponse tokenResponse = JsonSerialization.readValue(json, AccessTokenResponse.class);
                if (Time.currentTime() < tokenResponse.getExpiresIn()) {
                    return tokenResponse.getToken();
                }
                AdapterConfig config = getConfig();
                KeycloakInstalled installed = new KeycloakInstalled(KeycloakDeploymentBuilder.build(config));
                installed.refreshToken(tokenResponse.getRefreshToken());
                processResponse(installed, client);
                return tokenResponse.getToken();
            } catch (Exception e) {
                File tokenFile = getTokenFilePath(client);
                if (tokenFile.exists()) {
                    tokenFile.delete();
                }

                return null;
            }
        }
        return null;

    }

    public String readRefreshToken(String client) throws Exception {
        String json = getTokenResponse(client);
        if (json == null) return null;


        if (json != null) {
            try {
                AccessTokenResponse tokenResponse = JsonSerialization.readValue(json, AccessTokenResponse.class);
                return tokenResponse.getRefreshToken();
            } catch (Exception e) {
                if (debug) {
                    e.printStackTrace();
                }
                File tokenFile = getTokenFilePath(client);
                if (tokenFile.exists()) {
                    tokenFile.delete();
                }

                return null;
            }
        }
        return null;

    }


    private String getTokenResponse(String client) {
        File tokenFile = getTokenFilePath(client);
        try {
            return readFile(tokenFile);
        } catch (Exception e) {
            if (debug) {
                System.err.println("Failed to read encrypted file");
                e.printStackTrace();
            }
            if (tokenFile.exists()) tokenFile.delete();
            return null;
        }
    }


    public void token() throws Exception {
        KeycloakInstalled.console().stderrOutput();

        checkEnv();
        String masterClient = getMasterClient();
        String client = masterClient;
        if (args.length > 1) {
            client = args[1];
        }
        //System.err.println("readToken: " + client);
        String token = readToken(client);
        if (token != null) {
            System.out.print(token);
            return;
        }
        if (token == null && client.equals(masterClient)) {
            //System.err.println("not logged in, logging in.");
            doConsoleLogin();
            token = readToken(client);
            if (token != null) {
                System.out.print(token);
                return;
            }

        }
        String masterToken = readToken(masterClient);
        if (masterToken == null) {
            //System.err.println("not logged in, logging in.");
            doConsoleLogin();
            masterToken = readToken(masterClient);
            if (masterToken == null) {
                System.err.println("Login failed.  Cannot retrieve token");
                System.exit(1);
            }
        }

        //System.err.println("exchange: " + client);
        Client httpClient = getHttpClient();

        WebTarget exchangeUrl = httpClient.target(getServer())
                .path("/realms")
                .path(getRealm())
                .path("protocol/openid-connect/token");

        Form form = new Form()
                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                .param(OAuth2Constants.CLIENT_ID, masterClient)
                .param(OAuth2Constants.SUBJECT_TOKEN, masterToken)
                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE)
                .param(OAuth2Constants.AUDIENCE, client);
        if (getMasterClientSecret() != null) {
            form.param(OAuth2Constants.CLIENT_SECRET, getMasterClientSecret());
        }
        Response response = exchangeUrl.request().post(Entity.form(
                form
        ));

        if (response.getStatus() == 401 || response.getStatus() == 403) {
            response.close();
            System.err.println("Not allowed to exchange for client token");
            System.exit(1);
        }

        if (response.getStatus() != 200) {
            if (response.getMediaType() != null && response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                try {
                    String json = response.readEntity(String.class);
                    OAuth2ErrorRepresentation error = JsonSerialization.readValue(json, OAuth2ErrorRepresentation.class);
                    System.err.println("Failed to exchange token: " + error.getError() + ". " + error.getErrorDescription());
                    System.exit(1);
                } catch (Exception ignore) {
                    ignore.printStackTrace();

                }
            }

            response.close();
            System.err.println("Unknown error exchanging for client token: " + response.getStatus());
            System.exit(1);
        }

        String json = response.readEntity(String.class);
        response.close();
        AccessTokenResponse tokenResponse = JsonSerialization.readValue(json, AccessTokenResponse.class);
        if (tokenResponse.getToken() != null) {
            getTokenDirectory().mkdirs();
            tokenResponse.setExpiresIn(Time.currentTime() + tokenResponse.getExpiresIn());
            tokenResponse.setIdToken(null);
            json = JsonSerialization.writeValueAsString(tokenResponse);
            writeFile(getTokenFilePath(client), json);
            System.out.printf(tokenResponse.getToken());
        } else {
            System.err.println("Error processing token");
            System.exit(1);
        }
    }

    protected String getMasterClientSecret() {
        return getProperty("secret");
    }

    protected String getServer() {
        return getProperty("server");
    }

    protected String getRealm() {
        return getProperty("realm");
    }

    public String getProperty(String name) {
        return getConfigProperties().get(name);
    }

    protected boolean forceLogin() {
        return args.length > 0 && args[0].equals("-f");

    }

    public Client getHttpClient() {
        return new ResteasyClientBuilder().disableTrustManager().build();
    }

    public void login() throws Exception {
        checkEnv();
        this.args = Arrays.copyOfRange(this.args, 1, this.args.length);
        for (String arg : args) {
            if (arg.equals("-f") || arg.equals("-force")) {
                forceLogin = true;
                this.args = Arrays.copyOfRange(this.args, 1, this.args.length);
            } else if (arg.equals("-browser") || arg.equals("-b")) {
                browserLogin = true;
                this.args = Arrays.copyOfRange(this.args, 1, this.args.length);
            } else {
                System.err.println("Illegal argument: " + arg);
                printHelp();
                System.exit(1);
            }
        }

        String masterClient = getMasterClient();
        if (!forceLogin && readToken(masterClient) != null) {
            KeycloakInstalled.console().writer().println("Already logged in.  `kcinit -f` to force relogin");
            return;
        }
        doConsoleLogin();
        KeycloakInstalled.console().writer().println("Login successful!");
    }

    public void doConsoleLogin() throws Exception {
        String masterClient = getMasterClient();
        AdapterConfig config = getConfig();
        KeycloakInstalled installed = new KeycloakInstalled(KeycloakDeploymentBuilder.build(config));
        //System.err.println("calling loginCommandLine");
        if (!installed.loginCommandLine()) {
            System.exit(1);
        }
        processResponse(installed, masterClient);
    }

    private String getMasterClient() {
        return getProperty("client");
    }

    private void processResponse(KeycloakInstalled installed, String client) throws IOException {
        AccessTokenResponse tokenResponse = installed.getTokenResponse();
        tokenResponse.setExpiresIn(Time.currentTime() + tokenResponse.getExpiresIn());
        tokenResponse.setIdToken(null);
        String json = JsonSerialization.writeValueAsString(tokenResponse);
        getTokenDirectory().mkdirs();
        writeFile(getTokenFilePath(client), json);
    }

    public void logout() throws Exception {
        String token = readRefreshToken(getMasterClient());
        if (token != null) {
            try {
                KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getConfig());
                ServerRequest.invokeLogout(deployment, token);
            } catch (Exception e) {
                if (debug) {
                    e.printStackTrace();
                }
            }

        }
        if (getTokenDirectory().exists()) {
            for (File fp : getTokenDirectory().listFiles()) fp.delete();
        }
    }
    public void uninstall() throws Exception {
        File configFile = getConfigFile();
        if (configFile.exists()) configFile.delete();
        if (getTokenDirectory().exists()) {
            for (File fp : getTokenDirectory().listFiles()) fp.delete();
        }
    }
}
