/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.util;

/**
 *
 * @author pmensik
 */
public class URL {
    
    public static final String BASE_URL = "http://localhost:8080/auth/admin/master/console/index.html";
    
    public static String SETTINGS_GENERAL_SETTINGS = BASE_URL + "#/realms/%s";
    public static String SETTINGS_ROLES = BASE_URL + "#/realms/%s/roles";
    public static String SETTINGS_LOGIN = BASE_URL + "#/realms/%s/login-settings";
    public static String SETTINGS_SOCIAL = BASE_URL + "#/realms/%s/social-settings";
    
}
