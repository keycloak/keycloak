/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.core.AbstractKeycloakInstalled;
import org.keycloak.common.VerificationException;

class KeycloakInstalledCommandLine extends AbstractKeycloakInstalled {

    private static final Pattern callbackPattern = Pattern.compile("callback\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern paramPattern = Pattern.compile("param=\"([^\"]+)\"\\s+label=\"([^\"]+)\"\\s+mask=(\\S+)");
    private static final Pattern codePattern = Pattern.compile("code=([^&]+)");
    private ResteasyClient resteasyClient;

    public KeycloakInstalledCommandLine() {
    }

    public KeycloakInstalledCommandLine(InputStream config) {
        super(config);
    }

    public KeycloakInstalledCommandLine(KeycloakDeployment deployment) {
        super(deployment);
    }
    
    public boolean loginCommandLine() throws IOException, ServerRequest.HttpFailure, VerificationException {
        String redirectUri = "urn:ietf:wg:oauth:2.0:oob";

        return loginCommandLine(redirectUri);
    }

    /**
     * Experimental proprietary WWW-Authentication challenge protocol. WWW-Authentication: X-Text-Form-Challenge callback="{url}" param="{param-name}" label="{param-display-label}"
     *
     * @param redirectUri
     * @return
     * @throws IOException
     * @throws ServerRequest.HttpFailure
     * @throws VerificationException
     */
    public boolean loginCommandLine(String redirectUri) throws IOException, ServerRequest.HttpFailure, VerificationException {
        String authUrl = getDeployment().getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, getDeployment().getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam("display", "console")
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
                .build().toString();
        ResteasyClient client = createResteasyClient();
        try {
            //System.err.println("initial request");
            Response response = client.target(authUrl).request().get();
            while (true) {
                if (response.getStatus() == 403) {
                    if (response.getMediaType() != null) {
                        String splash = response.readEntity(String.class);
                        console().writer().println(splash);
                    } else {
                        System.err.println("Forbidden to login");
                    }
                    return false;
                } else {
                    if (response.getStatus() == 401) {
                        String authenticationHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
                        if (authenticationHeader == null) {
                            System.err.println("Failure:  Invalid protocol.  No WWW-Authenticate header");
                            return false;
                        }
                        //System.err.println("got header: " + authenticationHeader);
                        if (!authenticationHeader.contains("X-Text-Form-Challenge")) {
                            System.err.println("Failure:  Invalid WWW-Authenticate header.");
                            return false;
                        }
                        if (response.getMediaType() != null) {
                            String splash = response.readEntity(String.class);
                            console().writer().println(splash);
                        } else {
                            response.close();
                        }
                        Matcher m = callbackPattern.matcher(authenticationHeader);
                        if (!m.find()) {
                            System.err.println("Failure: Invalid WWW-Authenticate header.");
                            return false;
                        }
                        String callback = m.group(1);
                        //System.err.println("callback: " + callback);
                        m = paramPattern.matcher(authenticationHeader);
                        Form form = new Form();
                        while (m.find()) {
                            String param = m.group(1);
                            String label = m.group(2);
                            String mask = m.group(3).trim();
                            boolean maskInput = mask.equals("true");
                            String value = null;
                            if (maskInput) {
                                char[] txt = console().readPassword(label);
                                value = new String(txt);
                            } else {
                                value = console().readLine(label);
                            }
                            form.param(param, value);
                        }
                        response.close();
                        client.close();
                        client = createResteasyClient();
                        response = client.target(callback).request().post(Entity.form(form));
                    } else {
                        if (response.getStatus() == 302) {
                            int redirectCount = 0;
                            do {
                                String location = response.getLocation().toString();
                                Matcher m = codePattern.matcher(location);
                                if (!m.find()) {
                                    response.close();
                                    client.close();
                                    client = createResteasyClient();
                                    response = client.target(location).request().get();
                                } else {
                                    response.close();
                                    client.close();
                                    String code = m.group(1);
                                    processCode(code, redirectUri);
                                    return true;
                                }
                                if (response.getStatus() == 302 && redirectCount++ > 4) {
                                    System.err.println("Too many redirects.  Aborting");
                                    return false;
                                }
                            } while (response.getStatus() == 302);
                        } else {
                            System.err.println("Unknown response from server: " + response.getStatus());
                            return false;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            client.close();

        }
    }

    protected ResteasyClient getResteasyClient() {
        if (this.resteasyClient == null) {
            this.resteasyClient = createResteasyClient();
        }
        return this.resteasyClient;
    }

    protected ResteasyClient createResteasyClient() {
        return new ResteasyClientBuilder()
                .connectionCheckoutTimeout(1, TimeUnit.HOURS)
                .connectionTTL(1, TimeUnit.HOURS)
                .socketTimeout(1, TimeUnit.HOURS)
                .disableTrustManager().build();
    }

    public void setResteasyClient(ResteasyClient resteasyClient) {
        this.resteasyClient = resteasyClient;
    }


    public static class Console {

        protected java.io.Console console = System.console();
        protected PrintWriter writer;
        protected BufferedReader reader;

        static Console SINGLETON = new Console();

        protected Console() {
        }

        public PrintWriter writer() {
            if (console == null) {
                if (writer == null) {
                    writer = new PrintWriter(System.err, true);
                }
                return writer;
            }
            return console.writer();
        }

        public Reader reader() {
            if (console == null) {
                return getReader();
            }
            return console.reader();
        }

        protected BufferedReader getReader() {
            if (reader != null) {
                return reader;
            }
            reader = new BufferedReader(new BufferedReader(new InputStreamReader(System.in)));
            return reader;
        }

        public Console format(String fmt, Object... args) {
            if (console == null) {
                writer().format(fmt, args);
                return this;
            }
            console.format(fmt, args);
            return this;
        }

        public Console printf(String format, Object... args) {
            if (console == null) {
                writer().printf(format, args);
                return this;
            }
            console.printf(format, args);
            return this;
        }

        public String readLine(String fmt, Object... args) {
            if (console == null) {
                format(fmt, args);
                return readLine();
            }
            return console.readLine(fmt, args);
        }

        public boolean confirm(String fmt, Object... args) {
            String prompt = "";
            while (!"y".equals(prompt) && !"n".equals(prompt)) {
                prompt = readLine(fmt, args);
            }
            return "y".equals(prompt);

        }

        public String prompt(String fmt, Object... args) {
            String prompt = "";
            while (prompt.equals("")) {
                prompt = readLine(fmt, args).trim();
            }
            return prompt;

        }

        public String passwordPrompt(String fmt, Object... args) {
            String prompt = "";
            while (prompt.equals("")) {
                char[] val = readPassword(fmt, args);
                prompt = new String(val);
                prompt = prompt.trim();
            }
            return prompt;

        }

        public String readLine() {
            if (console == null) {
                try {
                    return getReader().readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return console.readLine();
        }

        public char[] readPassword(String fmt, Object... args) {
            if (console == null) {
                return readLine(fmt, args).toCharArray();

            }
            return console.readPassword(fmt, args);
        }

        public char[] readPassword() {
            if (console == null) {
                return readLine().toCharArray();
            }
            return console.readPassword();
        }

        public void flush() {
            if (console == null) {
                System.err.flush();
                return;
            }
            console.flush();
        }

        public void stderrOutput() {
            //System.err.println("not using System.console()");
            console = null;
        }
    }

    public static Console console() {
        return Console.SINGLETON;
    }
}
