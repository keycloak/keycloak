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

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.rotation.AdapterRSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakInstalled {

    public interface HttpResponseWriter {
        void success(PrintWriter pw, KeycloakInstalled ki);
        void failure(PrintWriter pw, KeycloakInstalled ki);
    }

    private static final String KEYCLOAK_JSON = "META-INF/keycloak.json";

    private KeycloakDeployment deployment;

    private enum Status {
        LOGGED_MANUAL, LOGGED_DESKTOP
    }

    private AccessTokenResponse tokenResponse;
    private String tokenString;
    private String idTokenString;
    private IDToken idToken;
    private AccessToken token;
    private String refreshToken;
    private Status status;
    private Locale locale;
    private HttpResponseWriter loginResponseWriter;
    private HttpResponseWriter logoutResponseWriter;
    Pattern callbackPattern = Pattern.compile("callback\\s*=\\s*\"([^\"]+)\"");
    Pattern paramPattern = Pattern.compile("param=\"([^\"]+)\"\\s+label=\"([^\"]+)\"\\s+mask=(\\S+)");
    Pattern codePattern = Pattern.compile("code=([^&]+)");



    public KeycloakInstalled() {
        InputStream config = Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYCLOAK_JSON);
        deployment = KeycloakDeploymentBuilder.build(config);
    }

    public KeycloakInstalled(InputStream config) {
        deployment = KeycloakDeploymentBuilder.build(config);
    }

    public KeycloakInstalled(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    private static HttpResponseWriter defaultLoginWriter = new HttpResponseWriter() {
        @Override
        public void success(PrintWriter pw, KeycloakInstalled ki) {
            pw.println("HTTP/1.1 200 OK");
            pw.println("Content-Type: text/html");
            pw.println();
            pw.println("<html><h1>Login completed.</h1><div>");
            pw.println("This browser will remain logged in until you close it, logout, or the session expires.");
            pw.println("</div></html>");
            pw.flush();

        }

        @Override
        public void failure(PrintWriter pw, KeycloakInstalled ki) {
            pw.println("HTTP/1.1 200 OK");
            pw.println("Content-Type: text/html");
            pw.println();
            pw.println("<html><h1>Login attempt failed.</h1><div>");
            pw.println("</div></html>");
            pw.flush();

        }
    };
    private static HttpResponseWriter defaultLogoutWriter = new HttpResponseWriter() {
        @Override
        public void success(PrintWriter pw, KeycloakInstalled ki) {
            pw.println("HTTP/1.1 200 OK");
            pw.println("Content-Type: text/html");
            pw.println();
            pw.println("<html><h1>Logout completed.</h1><div>");
            pw.println("You may close this browser tab.");
            pw.println("</div></html>");
            pw.flush();

        }

        @Override
        public void failure(PrintWriter pw, KeycloakInstalled ki) {
            pw.println("HTTP/1.1 200 OK");
            pw.println("Content-Type: text/html");
            pw.println();
            pw.println("<html><h1>Logout failed.</h1><div>");
            pw.println("You may close this browser tab.");
            pw.println("</div></html>");
            pw.flush();

        }
    };

    public HttpResponseWriter getLoginResponseWriter() {
        if (loginResponseWriter == null) {
            return defaultLoginWriter;
        } else {
            return loginResponseWriter;
        }
    }

    public HttpResponseWriter getLogoutResponseWriter() {
        if (logoutResponseWriter == null) {
            return defaultLogoutWriter;
        } else {
            return logoutResponseWriter;
        }
    }

    public void setLoginResponseWriter(HttpResponseWriter loginResponseWriter) {
        this.loginResponseWriter = loginResponseWriter;
    }

    public void setLogoutResponseWriter(HttpResponseWriter logoutResponseWriter) {
        this.logoutResponseWriter = logoutResponseWriter;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void login() throws IOException, ServerRequest.HttpFailure, VerificationException, InterruptedException, OAuthErrorException, URISyntaxException {
        if (isDesktopSupported()) {
            loginDesktop();
        } else {
            loginManual();
        }
    }

    public void login(PrintStream printer, Reader reader) throws IOException, ServerRequest.HttpFailure, VerificationException, InterruptedException, OAuthErrorException, URISyntaxException {
        if (isDesktopSupported()) {
            loginDesktop();
        } else {
            loginManual(printer, reader);
        }
    }

    public void logout() throws IOException, InterruptedException, URISyntaxException {
        if (status == Status.LOGGED_DESKTOP) {
            logoutDesktop();
        }

        tokenString = null;
        token = null;

        idTokenString = null;
        idToken = null;

        refreshToken = null;

        status = null;
    }

    public void loginDesktop() throws IOException, VerificationException, OAuthErrorException, URISyntaxException, ServerRequest.HttpFailure, InterruptedException {
        CallbackListener callback = new CallbackListener(getLoginResponseWriter());
        callback.start();

        String redirectUri = "http://localhost:" + callback.server.getLocalPort();
        String state = UUID.randomUUID().toString();

        KeycloakUriBuilder builder = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
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

        status = Status.LOGGED_DESKTOP;
    }

    private void logoutDesktop() throws IOException, URISyntaxException, InterruptedException {
        CallbackListener callback = new CallbackListener(getLogoutResponseWriter());
        callback.start();

        String redirectUri = "http://localhost:" + callback.server.getLocalPort();

        String logoutUrl = deployment.getLogoutUrl()
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .build().toString();

        Desktop.getDesktop().browse(new URI(logoutUrl));

        callback.join();

        if (callback.errorException != null) {
            throw callback.errorException;
        }
    }

    public void loginManual() throws IOException, ServerRequest.HttpFailure, VerificationException {
        loginManual(System.out, new InputStreamReader(System.in));
    }

    public void loginManual(PrintStream printer, Reader reader) throws IOException, ServerRequest.HttpFailure, VerificationException {
        String redirectUri = "urn:ietf:wg:oauth:2.0:oob";

        String authUrl = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
                .build().toString();

        printer.println("Open the following URL in a browser. After login copy/paste the code back and press <enter>");
        printer.println(authUrl);
        printer.println();
        printer.print("Code: ");

        String code = readCode(reader);
        processCode(code, redirectUri);

        status = Status.LOGGED_MANUAL;
    }

    public boolean loginCommandLine() throws IOException, ServerRequest.HttpFailure, VerificationException {
        String redirectUri = "urn:ietf:wg:oauth:2.0:oob";

        return loginCommandLine(redirectUri);
    }



    /**
     * Experimental proprietary WWW-Authentication challenge protocol.
     * WWW-Authentication: X-Text-Form-Challenge callback="{url}" param="{param-name}" label="{param-display-label}"
     *
     * @param redirectUri
     * @return
     * @throws IOException
     * @throws ServerRequest.HttpFailure
     * @throws VerificationException
     */
    public boolean loginCommandLine(String redirectUri) throws IOException, ServerRequest.HttpFailure, VerificationException {
        String authUrl = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
                .build().toString();
        ResteasyClient client = new ResteasyClientBuilder().disableTrustManager().build();
        try {
            Response response = client.target(authUrl).request().get();
            if (response.getStatus() != 401) {
                return false;
            }
            while (true) {
                String authenticationHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
                if (authenticationHeader == null) {
                    return false;
                }
                if (!authenticationHeader.contains("X-Text-Form-Challenge")) {
                    return false;
                }
                if (response.getMediaType() != null) {
                    String splash = response.readEntity(String.class);
                    System.console().writer().println(splash);
                }
                Matcher m = callbackPattern.matcher(authenticationHeader);
                if (!m.find()) return false;
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
                        char[] txt = System.console().readPassword(label);
                        value = new String(txt);
                    } else {
                        value = System.console().readLine(label);
                    }
                    form.param(param, value);
                }
                response = client.target(callback).request().post(Entity.form(form));
                if (response.getStatus() == 401) continue;
                if (response.getStatus() != 302) return false;
                String location = response.getLocation().toString();
                m = codePattern.matcher(location);
                if (!m.find()) return false;
                String code = m.group(1);
                processCode(code, redirectUri);
                return true;
            }
        } finally {
            client.close();

        }
    }


    public String getTokenString() throws VerificationException, IOException, ServerRequest.HttpFailure {
        return tokenString;
    }

    public String getTokenString(long minValidity, TimeUnit unit) throws VerificationException, IOException, ServerRequest.HttpFailure {
        long expires = ((long) token.getExpiration()) * 1000 - unit.toMillis(minValidity);
        if (expires < System.currentTimeMillis()) {
            refreshToken();
        }

        return tokenString;
    }

    public void refreshToken() throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeRefresh(deployment, refreshToken);
        parseAccessToken(tokenResponse);
    }

    public void refreshToken(String refreshToken)  throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeRefresh(deployment, refreshToken);
        parseAccessToken(tokenResponse);

    }

    private void parseAccessToken(AccessTokenResponse tokenResponse) throws VerificationException {
        this.tokenResponse = tokenResponse;
        tokenString = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();
        idTokenString = tokenResponse.getIdToken();

        token = AdapterRSATokenVerifier.verifyToken(tokenString, deployment);
        if (idTokenString != null) {
            try {
                JWSInput input = new JWSInput(idTokenString);
                idToken = input.readJsonContent(IDToken.class);
            } catch (JWSInputException e) {
                throw new VerificationException();
            }
        }
    }

    public AccessToken getToken() {
        return token;
    }

    public IDToken getIdToken() {
        return idToken;
    }

    public String getIdTokenString() {
        return idTokenString;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public AccessTokenResponse getTokenResponse() {
        return tokenResponse;
    }

    public boolean isDesktopSupported() {
        return Desktop.isDesktopSupported();
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }



    private void processCode(String code, String redirectUri) throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeAccessCodeToToken(deployment, code, redirectUri, null);
        parseAccessToken(tokenResponse);
    }

    private String readCode(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();

        char cb[] = new char[1];
        while (reader.read(cb) != -1) {
            char c = cb[0];
            if ((c == ' ') || (c == '\n') || (c == '\r')) {
                break;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static class MaskingThread extends Thread {
        private volatile boolean stop;
        private char echochar = '*';

        public MaskingThread() {
        }

        /**
         * Begin masking until asked to stop.
         */
        public void run() {

            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            try {
                stop = true;
                while(stop) {
                    System.out.print("\010" + echochar);
                    try {
                        // attempt masking at this rate
                        Thread.currentThread().sleep(1);
                    }catch (InterruptedException iex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally { // restore the original priority
                Thread.currentThread().setPriority(priority);
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking() {
            this.stop = false;
        }
    }

    public static String readMasked(Reader reader) {
        MaskingThread et = new MaskingThread();
        Thread mask = new Thread(et);
        mask.start();

        BufferedReader in = new BufferedReader(reader);
        String password = "";

        try {
            password = in.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // stop masking
        et.stopMasking();
        // return the password entered by the user
        return password;
    }

    private String readLine(Reader reader, boolean mask) throws IOException {
        if (mask) {
            System.out.print(" ");
            return readMasked(reader);
        }

        StringBuilder sb = new StringBuilder();

        char cb[] = new char[1];
        while (reader.read(cb) != -1) {
            char c = cb[0];
            if ((c == '\n') || (c == '\r')) {
                break;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }


    public class CallbackListener extends Thread {

        private ServerSocket server;

        private String code;

        private String error;

        private String errorDescription;

        private IOException errorException;

        private String state;

        private Socket socket;

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

                if (error == null) {
                    writer.success(pw, KeycloakInstalled.this);
                } else {
                    writer.failure(pw, KeycloakInstalled.this);
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
