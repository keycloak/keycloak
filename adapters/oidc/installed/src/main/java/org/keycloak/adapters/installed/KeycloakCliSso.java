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

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakCliSso {

    public void mainCmd(String[] args) throws Exception {
        if (args.length != 1) {
            printHelp();
            return;
        }

        if (args[0].equalsIgnoreCase("login")) {
            login();
        } else if (args[0].equalsIgnoreCase("login-manual")) {
            loginManual();
        }
        /*
        else if (args[0].equalsIgnoreCase("login-cli")) {
            loginCli();
        }
        */
        else if (args[0].equalsIgnoreCase("token")) {
            token();
        } else if (args[0].equalsIgnoreCase("logout")) {
            logout();
        } else if (args[0].equalsIgnoreCase("env")) {
            System.out.println(System.getenv().toString());
        } else {
            printHelp();
        }
    }


    public void printHelp() {
        System.err.println("Commands:");
        System.err.println("  login - login with desktop browser if available, otherwise do manual login.  Output is access token.");
        System.err.println("  login-manual - manual login");
        //System.err.println("  login-cli - attempt Keycloak proprietary cli protocol.  Otherwise do normal login");
        System.err.println("  token - print access token if logged in");
        System.err.println("  logout - logout.");
        System.exit(1);
    }

    public AdapterConfig getConfig() {
        String url = System.getProperty("KEYCLOAK_AUTH_SERVER");
        if (url == null) {
            System.err.println("KEYCLOAK_AUTH_SERVER property not set");
            System.exit(1);
        }
        String realm = System.getProperty("KEYCLOAK_REALM");
        if (realm == null) {
            System.err.println("KEYCLOAK_REALM property not set");
            System.exit(1);

        }
        String client = System.getProperty("KEYCLOAK_CLIENT");
        if (client == null) {
            System.err.println("KEYCLOAK_CLIENT property not set");
            System.exit(1);
        }
        String secret = System.getProperty("KEYCLOAK_CLIENT_SECRET");



        AdapterConfig config = new AdapterConfig();
        config.setAuthServerUrl(url);
        config.setRealm(realm);
        config.setResource(client);
        config.setSslRequired("external");
        if (secret != null) {
            Map<String, Object> creds = new HashMap<>();
            creds.put("secret", secret);
            config.setCredentials(creds);
        } else {
            config.setPublicClient(true);
        }
        return config;
    }

    public boolean checkToken(boolean outputToken) throws Exception {
        String token = getTokenResponse();
        if (token == null) return false;


        if (token != null) {
            Matcher m = Pattern.compile("\\{.*\\}\\z").matcher(token);
            if (m.find()) {
                String json = m.group(0);
                try {
                    AccessTokenResponse tokenResponse = JsonSerialization.readValue(json, AccessTokenResponse.class);
                    if (Time.currentTime() < tokenResponse.getExpiresIn()) {
                        return true;
                    }
                    AdapterConfig config = getConfig();
                    KeycloakInstalled installed = new KeycloakInstalled(KeycloakDeploymentBuilder.build(config));
                    installed.refreshToken(tokenResponse.getRefreshToken());
                    processResponse(installed, outputToken);
                    return true;
                } catch (Exception e) {
                    System.err.println("Error processing existing token");
                    e.printStackTrace();
                }

            }
        }
        return false;

    }

    private String getTokenResponse() throws IOException {
        String token = null;
        File tokenFile = getTokenFilePath();
        if (tokenFile.exists()) {
            FileInputStream fis = new FileInputStream(tokenFile);
            byte[] data = new byte[(int) tokenFile.length()];
            fis.read(data);
            fis.close();
            token = new String(data, "UTF-8");
        }
        return token;
    }

    public void token() throws Exception {
        String token = getTokenResponse();
        if (token == null) {
            System.err.println("There is no token for client");
            System.exit(1);
        } else {
            Matcher m = Pattern.compile("\\{.*\\}\\z").matcher(token);
            if (m.find()) {
                String json = m.group(0);
                try {
                    AccessTokenResponse tokenResponse = JsonSerialization.readValue(json, AccessTokenResponse.class);
                    if (Time.currentTime() < tokenResponse.getExpiresIn()) {
                        System.out.println(tokenResponse.getToken());
                        return;
                    } else {
                        System.err.println("token in response file is expired");
                        System.exit(1);
                    }
                } catch (Exception e) {
                    System.err.println("Failure processing token response file");
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                System.err.println("Could not find json within token response file");
                System.exit(1);
            }
        }
    }

    public void login() throws Exception {
        if (checkToken(true)) return;
        AdapterConfig config = getConfig();
        KeycloakInstalled installed = new KeycloakInstalled(KeycloakDeploymentBuilder.build(config));
        installed.login();
        processResponse(installed, true);
    }

    public void loginCli() throws Exception {
        if (checkToken(false)) return;
        AdapterConfig config = getConfig();
        KeycloakInstalled installed = new KeycloakInstalled(KeycloakDeploymentBuilder.build(config));
        if (!installed.loginCommandLine()) installed.login();
        processResponse(installed, false);
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

    public File getTokenDirectory() {
        return Paths.get(getHome(), System.getProperty("basepath", ".keycloak-sso"), System.getProperty("KEYCLOAK_REALM")).toFile();
    }

    public File getTokenFilePath() {
        return Paths.get(getHome(), System.getProperty("basepath", ".keycloak-sso"), System.getProperty("KEYCLOAK_REALM"), System.getProperty("KEYCLOAK_CLIENT") + ".json").toFile();
    }

    private void processResponse(KeycloakInstalled installed, boolean outputToken) throws IOException {
        AccessTokenResponse tokenResponse = installed.getTokenResponse();
        tokenResponse.setExpiresIn(Time.currentTime() + tokenResponse.getExpiresIn());
        tokenResponse.setIdToken(null);
        String output = JsonSerialization.writeValueAsString(tokenResponse);
        getTokenDirectory().mkdirs();
        FileOutputStream fos = new FileOutputStream(getTokenFilePath());
        fos.write(output.getBytes("UTF-8"));
        fos.flush();
        fos.close();
        if (outputToken) System.out.println(tokenResponse.getToken());
    }

    public void loginManual() throws Exception {
        if (checkToken(true)) return;
        AdapterConfig config = getConfig();
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(config);
        KeycloakInstalled installed = new KeycloakInstalled(deployment);
        installed.loginManual();
        processResponse(installed, true);
    }

    public void logout() throws Exception {
        String token = getTokenResponse();
        if (token != null) {
            Matcher m = Pattern.compile("\\{.*\\}\\z").matcher(token);
            if (m.find()) {
                String json = m.group(0);
                try {
                    AccessTokenResponse tokenResponse = JsonSerialization.readValue(json, AccessTokenResponse.class);
                    if (Time.currentTime() > tokenResponse.getExpiresIn()) {
                        System.err.println("Login is expired");
                        System.exit(1);
                    }
                    AdapterConfig config = getConfig();
                    KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(config);
                    ServerRequest.invokeLogout(deployment, tokenResponse.getRefreshToken());
                    for (File fp : getTokenDirectory().listFiles()) fp.delete();
                    System.out.println("logout complete");
                } catch (Exception e) {
                    System.err.println("Failure processing token response file");
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                System.err.println("Could not find json within token response file");
                System.exit(1);
            }
        } else {
            System.err.println("Not logged in");
            System.exit(1);
        }
    }
}
