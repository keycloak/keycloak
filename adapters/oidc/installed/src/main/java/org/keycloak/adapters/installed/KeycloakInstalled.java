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
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.RandomString;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
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
    private ResteasyClient resteasyClient;
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

    public HttpResponseWriter getLoginResponseWriter() {
        return null;
    }

    public HttpResponseWriter getLogoutResponseWriter() {
        return null;
    }

    public void setLoginResponseWriter(HttpResponseWriter loginResponseWriter) {
        this.loginResponseWriter = loginResponseWriter;
    }

    public void setLogoutResponseWriter(HttpResponseWriter logoutResponseWriter) {
        this.logoutResponseWriter = logoutResponseWriter;
    }

    public void setResteasyClient(ResteasyClient resteasyClient) {
        this.resteasyClient = resteasyClient;
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
        Pkce pkce = deployment.isPkce() ? generatePkce() : null;

        String authUrl = createAuthUrl(redirectUri, state, pkce);

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

        processCode(callback.code, redirectUri, pkce);

        status = Status.LOGGED_DESKTOP;
    }

    protected String createAuthUrl(String redirectUri, String state, Pkce pkce) {

        KeycloakUriBuilder builder = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID);

        if (state != null) {
            builder.queryParam(OAuth2Constants.STATE, state);
        }

        if (locale != null) {
            builder.queryParam(OAuth2Constants.UI_LOCALES_PARAM, locale.getLanguage());
        }

        if (pkce != null) {
            builder.queryParam(OAuth2Constants.CODE_CHALLENGE, pkce.getCodeChallenge());
            builder.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, "S256");
        }

        return builder.build().toString();
    }

    protected Pkce generatePkce(){
        return Pkce.generatePkce();
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

        Pkce pkce = generatePkce();

        String authUrl = createAuthUrl(redirectUri, null, pkce);

        printer.println("Open the following URL in a browser. After login copy/paste the code back and press <enter>");
        printer.println(authUrl);
        printer.println();
        printer.print("Code: ");

        String code = readCode(reader);
        processCode(code, redirectUri, pkce);

        status = Status.LOGGED_MANUAL;
    }

    public static class Console {
        protected java.io.Console console = System.console();
        protected PrintWriter writer;
        protected BufferedReader reader;

        static Console SINGLETON = new Console();

        private Console() {
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
            if (reader != null) return reader;
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
                } else if (response.getStatus() == 401) {
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
                } else if (response.getStatus() == 302) {
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
                            processCode(code, redirectUri, null);
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


    public String getTokenString() {
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

    public void refreshToken(String refreshToken) throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeRefresh(deployment, refreshToken);
        parseAccessToken(tokenResponse);

    }

    private void parseAccessToken(AccessTokenResponse tokenResponse) throws VerificationException {
        this.tokenResponse = tokenResponse;
        tokenString = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();
        idTokenString = tokenResponse.getIdToken();

        AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenString, idTokenString, deployment);
        token = tokens.getAccessToken();
        idToken = tokens.getIdToken();
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


    private void processCode(String code, String redirectUri, Pkce pkce) throws IOException, ServerRequest.HttpFailure, VerificationException {

        AccessTokenResponse tokenResponse = ServerRequest.invokeAccessCodeToToken(deployment, code, redirectUri, null, pkce == null ? null : pkce.getCodeVerifier());
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
                if (writer != null) {
                    System.err.println("Using a writer is deprecated.  Please remove its usage.  This is now handled by endpoint on server");
                }

                if (error == null) {
                     if (writer != null) {
                         writer.success(pw, KeycloakInstalled.this);
                     } else {
                         pw.println("HTTP/1.1 302 Found");
                         pw.println("Location: " + deployment.getTokenUrl().replace("/token", "/delegated"));

                     }
                } else {
                    if (writer != null) {
                        writer.failure(pw, KeycloakInstalled.this);
                    } else {
                        pw.println("HTTP/1.1 302 Found");
                        pw.println("Location: " + deployment.getTokenUrl().replace("/token", "/delegated?error=true"));

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

    public static class Pkce {

        // https://tools.ietf.org/html/rfc7636#section-4.1
        public static final int PKCE_CODE_VERIFIER_MAX_LENGTH = 128;

        private final String codeChallenge;
        private final String codeVerifier;

        public Pkce(String codeVerifier, String codeChallenge) {
            this.codeChallenge = codeChallenge;
            this.codeVerifier = codeVerifier;
        }

        public String getCodeChallenge() {
            return codeChallenge;
        }

        public String getCodeVerifier() {
            return codeVerifier;
        }

        public static Pkce generatePkce() {
            try {
                String codeVerifier = new RandomString(PKCE_CODE_VERIFIER_MAX_LENGTH, new SecureRandom()).nextString();
                String codeChallenge = generateS256CodeChallenge(codeVerifier);
                return new Pkce(codeVerifier, codeChallenge);
            } catch (Exception ex){
                throw new RuntimeException("Could not generate PKCE", ex);
            }
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        private static String generateS256CodeChallenge(String codeVerifier) throws Exception {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(codeVerifier.getBytes(StandardCharsets.ISO_8859_1));
            return Base64Url.encode(md.digest());
        }
    }
}
