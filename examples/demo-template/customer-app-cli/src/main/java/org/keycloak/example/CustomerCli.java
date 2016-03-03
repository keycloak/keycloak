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

package org.keycloak.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.KeycloakInstalled;
import org.keycloak.common.util.Time;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CustomerCli {

    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static BufferedReader br;

    private static KeycloakInstalled keycloak;

    public static void main(String[] args) throws Exception {
        keycloak = new KeycloakInstalled();
        br = new BufferedReader(new InputStreamReader(System.in));

        printHelp();
        printDivider();

        System.out.print("$ ");
        for (String s = br.readLine(); s != null; s = br.readLine()) {
            printDivider();

            try {
                if (s.equals("login")) {
                    keycloak.login(System.out, br);
                    System.out.println("Logged in: " + keycloak.getToken().getSubject());
                } else if (s.equals("logout")) {
                    keycloak.logout();
                    System.out.println("Logged out");
                } else if (s.equals("login-desktop")) {
                    keycloak.loginDesktop();
                    System.out.println("Logged in: " + keycloak.getToken().getSubject());
                } else if (s.equals("login-manual")) {
                    keycloak.loginManual(System.out, br);
                    System.out.println("Logged in: " + keycloak.getToken().getSubject());
                } else if (s.equals("profile")) {
                    profile();
                } else if (s.equals("customers")) {
                    customers();
                } else if (s.equals("token")) {
                    System.out.println(mapper.writeValueAsString(keycloak.getToken()));
                } else if (s.equals("id-token")) {
                    System.out.println(mapper.writeValueAsString(keycloak.getIdToken()));
                } else if (s.equals("refresh")) {
                    keycloak.refreshToken();
                    System.out.println("Token refreshed: expires at " + Time.toDate(keycloak.getToken().getExpiration()));
                } else if (s.equals("exit")) {
                    System.exit(0);
                } else {
                    printHelp();
                }
            } catch (ServerRequest.HttpFailure t) {
                System.out.println(t.getError());
            }catch (Throwable t) {
                System.out.println(t.getMessage() != null ? t.getMessage() : t.getClass().toString());
            }
            printDivider();

            System.out.print("$ ");
        }
    }

    public static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  login - login with desktop browser if available, otherwise do manual login");
        System.out.println("  login-manual - manual login");
        System.out.println("  login-desktop - desktop login");
        System.out.println("  token - show token details");
        System.out.println("  id-token - show ID token details");
        System.out.println("  profile - retrieve user profile");
        System.out.println("  customers - retrieve customers listing");
        System.out.println("  refresh - refresh token");
        System.out.println("  exit - exit");
    }

    public static void printDivider() {
        System.out.println("");
    }

    public static void profile() throws Exception {
        String accountUrl = keycloak.getDeployment().getAccountUrl();
        HttpGet get = new HttpGet(accountUrl);
        get.setHeader("Accept", "application/json");
        get.setHeader("Authorization", "Bearer " + keycloak.getTokenString(10, TimeUnit.SECONDS));

        HttpResponse response = keycloak.getDeployment().getClient().execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            print(response.getEntity().getContent());
        } else {
            System.out.println(response.getStatusLine().toString());
        }
    }

    public static void customers() throws Exception {
        String baseUrl = keycloak.getDeployment().getAuthServerBaseUrl();
        baseUrl = baseUrl.substring(0, baseUrl.indexOf('/', 8));

        String customersUrl = baseUrl + "/database/customers";
        HttpGet get = new HttpGet(customersUrl);
        get.setHeader("Accept", "application/json");
        get.setHeader("Authorization", "Bearer " + keycloak.getTokenString(10, TimeUnit.SECONDS));

        HttpResponse response = keycloak.getDeployment().getClient().execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            print(response.getEntity().getContent());
        } else {
            System.out.println(response.getStatusLine().toString());
        }
    }

    private static void print(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String l = br.readLine(); l != null; l = br.readLine()) {
            System.out.println(l);
        }
    }

}
