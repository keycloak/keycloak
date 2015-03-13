package org.keycloak.adapters.installed;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakInstalled {

    private static final String KEYCLOAK_JSON = "META-INF/keycloak.json";

    private KeycloakDeployment deployment;

    private enum Status {
        LOGGED_MANUAL, LOGGED_DESKTOP
    }

    private String tokenString;
    private String idTokenString;
    private IDToken idToken;
    private AccessToken token;
    private String refreshToken;
    private Status status;

    public KeycloakInstalled() {
        InputStream config = Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYCLOAK_JSON);
        deployment = KeycloakDeploymentBuilder.build(config);
    }

    public KeycloakInstalled(InputStream config) {
        deployment = KeycloakDeploymentBuilder.build(config);
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
        CallbackListener callback = new CallbackListener();
        callback.start();

        String redirectUri = "http://localhost:" + callback.server.getLocalPort();
        String state = UUID.randomUUID().toString();

        String authUrl = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.STATE, state)
                .build().toString();

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
        CallbackListener callback = new CallbackListener();
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
        CallbackListener callback = new CallbackListener();
        callback.start();

        String redirectUri = "urn:ietf:wg:oauth:2.0:oob";

        String authUrl = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .build().toString();

        printer.println("Open the following URL in a browser. After login copy/paste the code back and press <enter>");
        printer.println(authUrl);
        printer.println();
        printer.print("Code: ");

        String code = readCode(reader);
        processCode(code, redirectUri);

        status = Status.LOGGED_MANUAL;
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

    private void parseAccessToken(AccessTokenResponse tokenResponse) throws VerificationException {
        tokenString = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();
        idTokenString = tokenResponse.getIdToken();

        token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealm());
        if (idTokenString != null) {
            JWSInput input = new JWSInput(idTokenString);
            try {
                idToken = input.readJsonContent(IDToken.class);
            } catch (IOException e) {
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

    public class CallbackListener extends Thread {

        private ServerSocket server;

        private String code;

        private String error;

        private String errorDescription;

        private IOException errorException;

        private String state;

        public CallbackListener() throws IOException {
            server = new ServerSocket(0);
        }

        @Override
        public void run() {
            try {
                Socket socket = server.accept();

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

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                pw.println("Please close window and return to application");
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
