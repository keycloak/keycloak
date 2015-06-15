/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.ui.util;

/**
 *
 * @author Petr Mensik
 */
public class URL {

    public static final int KEYCLOAK_SERVER_PORT = Integer.parseInt(System.getProperty("keycloak.http.port", "8080"));
    public static final String AUTH_SERVER_BASE_URL = "http://localhost:" + KEYCLOAK_SERVER_PORT;
    public static final String AUTH_SERVER_URL = AUTH_SERVER_BASE_URL + "/auth";
    public static final String ADMIN_CONSOLE_URL = AUTH_SERVER_URL + "/admin/master/console/index.html";

    public static String SETTINGS_GENERAL_SETTINGS = ADMIN_CONSOLE_URL + "#/realms/%s";
    public static String SETTINGS_ROLES = ADMIN_CONSOLE_URL + "#/realms/%s/roles";
    public static String SETTINGS_LOGIN = ADMIN_CONSOLE_URL + "#/realms/%s/login-settings";
    public static String SETTINGS_SOCIAL = ADMIN_CONSOLE_URL + "#/realms/%s/social-settings";

    public static final int KEYCLOAK_ADAPTER_SERVER_PORT = Integer.parseInt(System.getProperty("keycloak.adapter.http.port", "8080"));
    public static final String APP_SERVER_BASE_URL = "http://localhost:" + KEYCLOAK_ADAPTER_SERVER_PORT;

}
