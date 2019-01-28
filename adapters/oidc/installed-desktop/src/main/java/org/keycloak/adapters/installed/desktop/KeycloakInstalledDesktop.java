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

package org.keycloak.adapters.installed.desktop;


import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.core.AbstractKeycloakInstalled;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeycloakUriBuilder;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakInstalledDesktop extends AbstractKeycloakInstalled {

    public interface HttpResponseWriter {
        void success(PrintWriter pw, KeycloakInstalledDesktop ki);

        void failure(PrintWriter pw, KeycloakInstalledDesktop ki);
    }

    private static final String KEYCLOAK_JSON = "META-INF/keycloak.json";


    private enum Status {
        LOGGED_MANUAL, LOGGED_DESKTOP
    }

    private Status status;
    private Locale locale;
    @Deprecated
    private HttpResponseWriter loginResponseWriter;
    @Deprecated
    private HttpResponseWriter logoutResponseWriter;

    Pattern callbackPattern = Pattern.compile("callback\\s*=\\s*\"([^\"]+)\"");
    Pattern paramPattern = Pattern.compile("param=\"([^\"]+)\"\\s+label=\"([^\"]+)\"\\s+mask=(\\S+)");
    Pattern codePattern = Pattern.compile("code=([^&]+)");


    public KeycloakInstalledDesktop() {
    }

    public KeycloakInstalledDesktop(InputStream config) {
        super(config);
    }

    public KeycloakInstalledDesktop(KeycloakDeployment deployment) {
        super(deployment);
    }

    @Deprecated
    public HttpResponseWriter getLoginResponseWriter() {
        return null;
    }

    @Deprecated
    public HttpResponseWriter getLogoutResponseWriter() {
        return null;
    }

    @Deprecated
    public void setLoginResponseWriter(HttpResponseWriter loginResponseWriter) {
        this.loginResponseWriter = loginResponseWriter;
    }

    @Deprecated
    public void setLogoutResponseWriter(HttpResponseWriter logoutResponseWriter) {
        this.logoutResponseWriter = logoutResponseWriter;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void login() throws IOException, VerificationException, OAuthErrorException, URISyntaxException, ServerRequest.HttpFailure, InterruptedException {
        CallbackListener callback = new CallbackListener(getLoginResponseWriter());
        callback.start();

        String redirectUri = "http://localhost:" + callback.server.getLocalPort();
        String state = UUID.randomUUID().toString();

        KeycloakUriBuilder builder = getDeployment().getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, getDeployment().getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.STATE, state)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID);
        if (locale != null) {
            builder.queryParam(OAuth2Constants.UI_LOCALES_PARAM, locale.getLanguage());
        }
        String authUrl = builder.build().toString();

        Desktop.getDesktop().browse(new URI(authUrl));

        callback.join();

        if (!state.equals(callback.state)) {
            throw new VerificationException("Invalid state");
        }

        if (callback.error != null) {
            throw new OAuthErrorException(callback.error, callback.errorDescription);
        }

        if (callback.errorException != null) {
            throw callback.errorException;
        }

        processCode(callback.code, redirectUri);
    }
    
    @Override
    public void logout() throws IOException, InterruptedException, URISyntaxException {
        logoutDesktop();
        super.logout();
    }
    
    private void logoutDesktop() throws IOException, URISyntaxException, InterruptedException {
        CallbackListener callback = new CallbackListener(getLogoutResponseWriter());
        callback.start();

        String redirectUri = "http://localhost:" + callback.server.getLocalPort();

        String logoutUrl = getDeployment().getLogoutUrl()
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .build().toString();

        Desktop.getDesktop().browse(new URI(logoutUrl));

        callback.join();

        if (callback.errorException != null) {
            throw callback.errorException;
        }
    }



    public class CallbackListener extends Thread {

        private ServerSocket server;

        private String code;

        private String error;

        private String errorDescription;

        private IOException errorException;

        private String state;

        private Socket socket;

        @Deprecated
        private HttpResponseWriter writer;

        public CallbackListener(HttpResponseWriter writer) throws IOException {
            this.writer = writer;
            server = new ServerSocket(0);
        }

        @Override
        public void run() {
            try {
                socket = server.accept();

                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String request = br.readLine();

                String url = request.split(" ")[1];
                if (url.indexOf('?') >= 0) {
                    url = url.split("\\?")[1];
                    String[] params = url.split("&");

                    for (String param : params) {
                        String[] p = param.split("=");
                        if (p[0].equals(OAuth2Constants.CODE)) {
                            code = p[1];
                        } else if (p[0].equals(OAuth2Constants.ERROR)) {
                            error = p[1];
                        } else if (p[0].equals("error-description")) {
                            errorDescription = p[1];
                        } else if (p[0].equals(OAuth2Constants.STATE)) {
                            state = p[1];
                        }
                    }
                }

                OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
                PrintWriter pw = new PrintWriter(out);
                if (writer != null) {
                    System.err.println("Using a writer is deprecated.  Please remove its usage.  This is now handled by endpoint on server");
                }

                if (error == null) {
                    if (writer != null) {
                        writer.success(pw, KeycloakInstalledDesktop.this);
                    } else {
                        pw.println("HTTP/1.1 302 Found");
                        pw.println("Location: " + getDeployment().getTokenUrl().replace("/token", "/delegated"));

                    }
                } else {
                    if (writer != null) {
                        writer.failure(pw, KeycloakInstalledDesktop.this);
                    } else {
                        pw.println("HTTP/1.1 302 Found");
                        pw.println("Location: " + getDeployment().getTokenUrl().replace("/token", "/delegated?error=true"));

                    }
                }
                pw.flush();
                socket.close();
            } catch (IOException e) {
                errorException = e;
            }

            try {
                server.close();
            } catch (IOException e) {
            }
        }

    }


}
