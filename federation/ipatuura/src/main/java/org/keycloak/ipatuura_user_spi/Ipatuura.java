/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.ipatuura_user_spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.ipatuura_user_spi.schemas.SCIMSearchRequest;
import org.keycloak.ipatuura_user_spi.schemas.SCIMUser;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

public class Ipatuura {
    private static final Logger logger = Logger.getLogger(Ipatuura.class);

    private final ComponentModel model;
    public static final String SCHEMA_CORE_USER = "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String SCHEMA_API_MESSAGES_SEARCHREQUEST = "urn:ietf:params:scim:api:messages:2.0:SearchRequest";

    String sessionid_cookie;
    String csrf_cookie;
    String csrf_value;
    Boolean logged_in = false;

    private final KeycloakSession session;

    public Ipatuura(KeycloakSession session, ComponentModel model) {
        this.model = model;
        this.session = session;
    }

    private void parseSetCookie(SimpleHttpResponse response) throws IOException {
        List<String> setCookieHeaders = response.getHeader("Set-Cookie");

        for (String h : setCookieHeaders) {
            String[] kv = h.split(";");
            for (String s : kv) {
                if (s.contains("csrftoken")) {
                    /* key=value */
                    csrf_cookie = s;
                    csrf_value = s.substring(s.lastIndexOf("=") + 1);
                } else if (s.contains("sessionid")) {
                    /* key=value */
                    sessionid_cookie = s;
                    csrf_cookie += String.format("; %s", sessionid_cookie);
                }
            }
        }
    }

    public Integer csrfAuthLogin() {

        SimpleHttpResponse response;

        /* Get inputs */
        String server = model.getConfig().getFirst("scimurl");
        String username = model.getConfig().getFirst("loginusername");
        String password = model.getConfig().getFirst("loginpassword");

        /* Execute GET to get initial csrftoken */
        String url = String.format("https://%s%s", server, "/admin/login/");

        try {
            response = SimpleHttp.create(session).doGet(url).asResponse();
            parseSetCookie(response);
            response.close();
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        /* Perform login POST */
        try {
            /* Here we retrieve the Response sessionid and csrftoken cookie */
            response = SimpleHttp.create(session).doPost(url).header("X-CSRFToken", csrf_value).header("Cookie", csrf_cookie)
                    .header("referer", url).param("username", username).param("password", password).asResponse();

            parseSetCookie(response);
            response.close();
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        this.logged_in = true;
        return 0;
    }

    public boolean isValid(String username, String password) {

        if (!this.logged_in) {
            this.csrfAuthLogin();
        }

        /* Build URL */
        String server = model.getConfig().getFirst("scimurl");
        String endpointurl = String.format("https://%s/creds/simple_pwd", server);

        logger.debugv("Sending POST request to {0}", endpointurl);
        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doPost(endpointurl).header("X-CSRFToken", this.csrf_value)
                .header("Cookie", this.csrf_cookie).header("SessionId", sessionid_cookie).header("referer", endpointurl)
                .param("username", username).param("password", password);
        try (SimpleHttpResponse response = simpleHttp.asResponse()){
            JsonNode result = response.asJson();
            return (result.get("result").get("validated").asBoolean());
        } catch (Exception e) {
            logger.debugv("Failed to authenticate user {0}: {1}", username, e);
            return false;
        }

    }

    public String gssAuth(String spnegoToken) {

        String server = model.getConfig().getFirst("scimurl");
        String endpointurl = String.format("https://%s/bridge/login_kerberos/", server);

        logger.debugv("Sending POST request to {0}", endpointurl);
        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doPost(endpointurl).header("Authorization", "Negotiate " + spnegoToken)
                .param("username", "");
        try (SimpleHttpResponse response = simpleHttp.asResponse()) {
            logger.debugv("Response status is {0}", response.getStatus());
            return response.getFirstHeader("Remote-User");
        } catch (Exception e) {
            logger.debugv("Failed to authenticate user with GSSAPI: {0}", e.toString());
            return null;
        }
    }

    public <T> SimpleHttpResponse clientRequest(String endpoint, String method, T entity) throws Exception {
        SimpleHttpResponse response = null;

        if (!this.logged_in) {
            this.csrfAuthLogin();
        }

        /* Build URL */
        String server = model.getConfig().getFirst("scimurl");
        String endpointurl;
        if (endpoint.contains("domain")) {
            endpointurl = String.format("https://%s/domains/v1/%s/", server, endpoint);
        } else {
            endpointurl = String.format("https://%s/scim/v2/%s", server, endpoint);
        }

        logger.debugv("Sending {0} request to {1}", method, endpointurl);

        try {
            switch (method) {
                case "GET":
                    response = SimpleHttp.create(session).doGet(endpointurl).header("X-CSRFToken", csrf_value)
                            .header("Cookie", csrf_cookie).header("SessionId", sessionid_cookie).asResponse();
                    break;
                case "DELETE":
                    response = SimpleHttp.create(session).doDelete(endpointurl).header("X-CSRFToken", csrf_value)
                            .header("Cookie", csrf_cookie).header("SessionId", sessionid_cookie).header("referer", endpointurl)
                            .asResponse();
                    break;
                case "POST":
                    /* Header is needed for domains endpoint only, but use it here anyway */
                    response = SimpleHttp.create(session).doPost(endpointurl).header("X-CSRFToken", this.csrf_value)
                            .header("Cookie", this.csrf_cookie).header("SessionId", sessionid_cookie)
                            .header("referer", endpointurl).json(entity).asResponse();
                    break;
                case "PUT":
                    response = SimpleHttp.create(session).doPut(endpointurl).header("X-CSRFToken", this.csrf_value)
                            .header("SessionId", sessionid_cookie).header("Cookie", this.csrf_cookie).json(entity).asResponse();
                    break;
                default:
                    logger.warn("Unknown HTTP method, skipping");
                    break;
            }
        } catch (Exception e) {
            throw new Exception();
        }

        /* Caller is responsible for executing .close() */
        return response;
    }

    private SCIMSearchRequest setupSearch(String username, String attribute) {
        List<String> schemas = new ArrayList<String>();
        SCIMSearchRequest search = new SCIMSearchRequest();
        String filter;

        schemas.add(SCHEMA_API_MESSAGES_SEARCHREQUEST);
        search.setSchemas(schemas);

        filter = String.format("%s eq \"%s\"", attribute, username);
        search.setFilter(filter);
        logger.debugv("filter: {0}", filter);
        logger.debugv("Schema: {0}", SCHEMA_API_MESSAGES_SEARCHREQUEST);

        return search;
    }

    private SCIMUser getUserByAttr(String username, String attribute) {
        SCIMSearchRequest newSearch = setupSearch(username, attribute);

        String usersSearchUrl = "Users/.search";
        SCIMUser user = null;

        SimpleHttpResponse response;
        try {
            response = clientRequest(usersSearchUrl, "POST", newSearch);
            user = response.asJson(SCIMUser.class);
            response.close();
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        return user;
    }

    public SCIMUser getUserByUsername(String username) {
        String attribute = "userName";
        return getUserByAttr(username, attribute);
    }

    public SCIMUser getUserByEmail(String username) {
        String attribute = "emails.value";
        return getUserByAttr(username, attribute);
    }

    public SCIMUser getUserByFirstName(String username) {
        String attribute = "name.givenName";
        return getUserByAttr(username, attribute);
    }

    public SCIMUser getUserByLastName(String username) {
        String attribute = "name.familyName";
        return getUserByAttr(username, attribute);
    }

    public SimpleHttpResponse deleteUser(String username) {
        SCIMUser userobj = getUserByUsername(username);
        SCIMUser.Resource user = userobj.getResources().get(0);

        String userIdUrl = String.format("Users/%s", user.getId());

        SimpleHttpResponse response;
        try {
            response = clientRequest(userIdUrl, "DELETE", null);
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        return response;
    }

    /*
     * Keycloak UserRegistrationProvider addUser() method only provides username as input, here we provide mostly dummy values
     * which will be replaced by actual user input via appropriate setter methods once in the returned UserModel
     */
    private SCIMUser.Resource setupUser(String username) {
        SCIMUser.Resource user = new SCIMUser.Resource();
        SCIMUser.Resource.Name name = new SCIMUser.Resource.Name();
        SCIMUser.Resource.Email email = new SCIMUser.Resource.Email();
        List<String> schemas = new ArrayList<String>();
        List<SCIMUser.Resource.Email> emails = new ArrayList<SCIMUser.Resource.Email>();
        List<SCIMUser.Resource.Group> groups = new ArrayList<SCIMUser.Resource.Group>();

        schemas.add(SCHEMA_CORE_USER);
        user.setSchemas(schemas);
        user.setUserName(username);
        user.setActive(true);
        user.setGroups(groups);

        name.setGivenName("dummyfirstname");
        name.setMiddleName("");
        name.setFamilyName("dummylastname");
        user.setName(name);

        email.setPrimary(true);
        email.setType("work");
        email.setValue("dummy@example.com");
        emails.add(email);
        user.setEmails(emails);

        return user;
    }

    public SimpleHttpResponse createUser(String username) {
        String usersUrl = "Users";

        SCIMUser.Resource newUser = setupUser(username);

        SimpleHttpResponse response;
        try {
            response = clientRequest(usersUrl, "POST", newUser);
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            return null;
        }

        return response;
    }

    private void setUserAttr(SCIMUser.Resource user, String attr, String value) {
        SCIMUser.Resource.Name name = user.getName();
        SCIMUser.Resource.Email email = new SCIMUser.Resource.Email();
        List<SCIMUser.Resource.Email> emails = new ArrayList<SCIMUser.Resource.Email>();

        switch (attr) {
            case UserModel.FIRST_NAME:
                name.setGivenName(value);
                user.setName(name);
                break;
            case UserModel.LAST_NAME:
                name.setFamilyName(value);
                user.setName(name);
                break;
            case UserModel.EMAIL:
                email.setValue(value);
                emails.add(email);
                user.setEmails(emails);
                break;
            case UserModel.USERNAME:
                /* Changing username not supported */
                break;
            default:
                logger.debug("Unknown user attribute to set: " + attr);
                break;
        }
    }

    public SimpleHttpResponse updateUser(Ipatuura ipatuura, String username, String attr, List<String> values) {
        logger.debug(String.format("Updating %s attribute for %s", attr, username));
        /* Get existing user */
        if (ipatuura.csrfAuthLogin() == null) {
            logger.error("Error during login");
        }

        SCIMUser userobj = getUserByUsername(username);
        SCIMUser.Resource user = userobj.getResources().get(0);

        /* Modify attributes */
        setUserAttr(user, attr, values.get(0));

        /* Update user in SCIM */
        String modifyUrl = String.format("Users/%s", user.getId());

        SimpleHttpResponse response;
        try {
            response = clientRequest(modifyUrl, "PUT", user);
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        return response;
    }

    public boolean getActive(SCIMUser user) {
        return user.getResources().get(0).getActive();
    }

    public String getEmail(SCIMUser user) {
        return user.getResources().get(0).getEmails().get(0).getValue();
    }

    public String getFirstName(SCIMUser user) {
        return user.getResources().get(0).getName().getGivenName();
    }

    public String getLastName(SCIMUser user) {
        return user.getResources().get(0).getName().getFamilyName();
    }

    public String getUserName(SCIMUser user) {
        return user.getResources().get(0).getUserName();
    }

    public String getId(SCIMUser user) {
        return user.getResources().get(0).getId();
    }

    public List<String> getGroupsList(SCIMUser user) {
        List<SCIMUser.Resource.Group> groups = user.getResources().get(0).getGroups();
        List<String> groupnames = new ArrayList<String>();

        for (SCIMUser.Resource.Group group : groups) {
            logger.debug("Retrieving group: " + group.getDisplay());
            groupnames.add(group.getDisplay());
        }

        return groupnames;
    }
}
