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
    
    public static final String BASE_URL = "http://localhost:8080/auth/admin/master/console/index.html";
    
    public static String SETTINGS_GENERAL_SETTINGS = BASE_URL + "#/realms/%s";
    public static String SETTINGS_ROLES = BASE_URL + "#/realms/%s/roles";
    public static String SETTINGS_LOGIN = BASE_URL + "#/realms/%s/login-settings";
    public static String SETTINGS_SOCIAL = BASE_URL + "#/realms/%s/social-settings";
    
}
