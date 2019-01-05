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

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.core.AbstractKeycloakInstalled;
import org.keycloak.adapters.installed.desktop.KeycloakInstalledDesktop;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

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

    private Locale locale;
    private HttpResponseWriter loginResponseWriter;
    private HttpResponseWriter logoutResponseWriter;
    private final KeycloakInstalledDesktop desktop;
    private final KeycloakInstalledManual manual;
    private final KeycloakInstalledCommandLine commandLine;
    private AbstractKeycloakInstalled currentInstalled;


    public KeycloakInstalled() {
        this(Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYCLOAK_JSON));
    }

    public KeycloakInstalled(InputStream config) {
        this(KeycloakDeploymentBuilder.build(config));
    }

    public KeycloakInstalled(KeycloakDeployment deployment) {
        this.deployment = deployment;
        this.desktop = new KeycloakInstalledDesktop(deployment);
        this.manual = new KeycloakInstalledManual(deployment);
        this.commandLine = new KeycloakInstalledCommandLine(deployment);
        this.currentInstalled = desktop;
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
        commandLine.setResteasyClient(resteasyClient);
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
        if (currentInstalled != null){
            currentInstalled.logout();
        }
    }

    public void loginDesktop() throws IOException, VerificationException, OAuthErrorException, URISyntaxException, ServerRequest.HttpFailure, InterruptedException {
        desktop.login();
        currentInstalled = desktop;
    }

    public void loginManual() throws IOException, ServerRequest.HttpFailure, VerificationException {
        manual.login();
        currentInstalled = manual;

    }

    public void loginManual(PrintStream printer, Reader reader) throws IOException, ServerRequest.HttpFailure, VerificationException {
        manual.login(printer, reader);
        currentInstalled = manual;
    }

    @Deprecated
    public static class Console extends KeycloakInstalledCommandLine.Console {

        static Console SINGLETON = new Console();

        private Console() {
        }
    }

    @Deprecated
    public static Console console() {
        return Console.SINGLETON;
    }

    public boolean loginCommandLine() throws IOException, ServerRequest.HttpFailure, VerificationException {
        return commandLine.loginCommandLine();
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
       return commandLine.loginCommandLine(redirectUri);
    }

    protected ResteasyClient getResteasyClient() {
        return commandLine.getResteasyClient();
    }

    protected ResteasyClient createResteasyClient() {
        return commandLine.createResteasyClient();
    }


    public String getTokenString() throws VerificationException, IOException, ServerRequest.HttpFailure {
        return currentInstalled.getTokenString();
    }

    public String getTokenString(long minValidity, TimeUnit unit) throws VerificationException, IOException, ServerRequest.HttpFailure {
        return currentInstalled.getTokenString(minValidity, unit);
    }

    public void refreshToken() throws IOException, ServerRequest.HttpFailure, VerificationException {
        currentInstalled.refreshToken();
    }

    public void refreshToken(String refreshToken) throws IOException, ServerRequest.HttpFailure, VerificationException {
        currentInstalled.refreshToken(refreshToken);
    }

    public AccessToken getToken() {
        return currentInstalled.getToken();
    }

    public IDToken getIdToken() {
        return currentInstalled.getIdToken();
    }

    public String getIdTokenString() {
        return currentInstalled.getIdTokenString();
    }

    public String getRefreshToken() {
        return currentInstalled.getRefreshToken();
    }

    public AccessTokenResponse getTokenResponse() {
        return currentInstalled.getTokenResponse();
    }

    public boolean isDesktopSupported() {
        return Desktop.isDesktopSupported();
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

}
