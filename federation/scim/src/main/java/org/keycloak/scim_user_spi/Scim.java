package org.keycloak.scim_user_spi;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.provider.util.SimpleHttp;

import org.keycloak.scim_user_spi.schemas.SCIMSearchRequest;
import org.keycloak.scim_user_spi.schemas.SCIMUser;
import org.keycloak.scim_user_spi.schemas.IntegrationDomain;

public class Scim {
    private static final Logger logger = Logger.getLogger(Scim.class);

    private final ComponentModel model;
    public static final String SCHEMA_CORE_USER = "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String SCHEMA_API_MESSAGES_SEARCHREQUEST = "urn:ietf:params:scim:api:messages:2.0:SearchRequest";

    String sessionid_cookie;
    String csrf_cookie;
    String csrf_value;
    Boolean logged_in = false;

    private final KeycloakSession session;

    public Scim(KeycloakSession session, ComponentModel model) {
        this.model = model;
        this.session = session;
    }

    private void parseSetCookie(SimpleHttp.Response response) throws IOException {
        List<String> setCookieHeaders = response.getHeader("Set-Cookie");

        for (String h : setCookieHeaders) {
            String[] kv = h.split("\\;");
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
        String url = "";
        String loginPage = "";
        SimpleHttp.Response response = null;

        /* Get inputs */
        String server = model.getConfig().getFirst("scimurl");
        String username = model.getConfig().getFirst("loginusername");
        String password = model.getConfig().getFirst("loginpassword");

        /* Execute GET to get initial csrftoken */
        url = String.format("https://%s%s", server, "/admin/login/");

        try {
            response = SimpleHttp.doGet(url, session).asResponse();

            parseSetCookie(response);

            response.close();
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        /* Perform login POST */
        try {
            /* Here we retrieve the Response sessionid and csrftoken cookie */
            response = SimpleHttp.doPost(url, session).header("X-CSRFToken", csrf_value).header("Cookie", csrf_cookie)
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
        SimpleHttp.Response response = null;
        com.fasterxml.jackson.databind.JsonNode result;
        if (this.logged_in == false) {
            this.csrfAuthLogin();
        }

        /* Build URL */

        String server = model.getConfig().getFirst("scimurl");
        String endpointurl = String.format("https://%s/creds/simple_pwd", server);

        logger.infov("Sending POST request to {0}", endpointurl);
        try {
            response = SimpleHttp.doPost(endpointurl, session).header("X-CSRFToken", this.csrf_value)
                    .header("Cookie", this.csrf_cookie).header("SessionId", sessionid_cookie).header("referer", endpointurl)
                    .param("username", username).param("password", password).asResponse();
            result = response.asJson();
            return (result.get("result").get("validated").asBoolean());
        } catch (Exception e) {
            logger.infov("Failed to authenticate user {0}: {1}", username, e);
            return false;
        }

    }

    public String gssAuth(String spnegoToken) {
        SimpleHttp.Response response = null;
        com.fasterxml.jackson.databind.JsonNode result;

        String server = model.getConfig().getFirst("scimurl");
        String endpointurl = String.format("https://%s/bridge/login_kerberos/", server);

        logger.infov("Sending POST request to {0}", endpointurl);
        try {
            response = SimpleHttp.doPost(endpointurl, session).header("Authorization", "Negotiate " + spnegoToken)
                    .param("username", "").asResponse();
            result = response.asJson();
            logger.infov("Response status is {0}", response.getStatus());
            String user = response.getFirstHeader("Remote-User");
            return user;
        } catch (Exception e) {
            logger.infov("Failed to authenticate user with GSSAPI: {0}", e.toString());
            return null;
        }
    }

    public boolean domainsRequest() {
        IntegrationDomain intgdomain = this.setupIntegrationDomain();

        SimpleHttp.Response response = null;
        com.fasterxml.jackson.databind.JsonNode result;

        String domainurl = "domain";

        try {
            response = clientRequest(domainurl, "POST", intgdomain);
            result = response.asJson();
            logger.infov("Result is {0}", result);
            return true;
        } catch (Exception e) {
            logger.errorv("Failed to add integration domain: {0}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean domainsRemove() {
        SimpleHttp.Response response = null;
        com.fasterxml.jackson.databind.JsonNode result;

        /* Currently only a single domain is supported */
        String domainurl = "domain/1";

        try {
            response = clientRequest(domainurl, "DELETE", null);
            /* Returns HttpStatus.SC_NO_CONTENT (HTTP 204) */
            logger.infov("Response status is {0}", response.getStatus());
            return true;
        } catch (Exception e) {
            logger.errorv("Failed to remove existing integration domain: {0}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public <T> SimpleHttp.Response clientRequest(String endpoint, String method, T entity) throws Exception {
        SimpleHttp.Response response = null;

        if (this.logged_in == false) {
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

        logger.infov("Sending {0} request to {1}", method.toString(), endpointurl);

        try {
            switch (method) {
                case "GET":
                    response = SimpleHttp.doGet(endpointurl, session).header("X-CSRFToken", csrf_value)
                            .header("Cookie", csrf_cookie).header("SessionId", sessionid_cookie).asResponse();
                    break;
                case "DELETE":
                    response = SimpleHttp.doDelete(endpointurl, session).header("X-CSRFToken", csrf_value)
                            .header("Cookie", csrf_cookie).header("SessionId", sessionid_cookie).header("referer", endpointurl)
                            .asResponse();
                    break;
                case "POST":
                    /* Header is needed for domains endpoint only, but use it here anyway */
                    response = SimpleHttp.doPost(endpointurl, session).header("X-CSRFToken", this.csrf_value)
                            .header("Cookie", this.csrf_cookie).header("SessionId", sessionid_cookie)
                            .header("referer", endpointurl).json(entity).asResponse();
                    break;
                case "PUT":
                    response = SimpleHttp.doPut(endpointurl, session).header("X-CSRFToken", this.csrf_value)
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
        logger.infov("filter: {0}", filter);
        logger.infov("Schema: {0}", SCHEMA_API_MESSAGES_SEARCHREQUEST);

        return search;
    }

    private IntegrationDomain setupIntegrationDomain() {
        IntegrationDomain intgdomain = new IntegrationDomain();

        intgdomain.setName(model.getConfig().getFirst("domainname"));
        intgdomain.setDescription(model.getConfig().getFirst("domaindesc"));
        intgdomain.setIntegrationDomainUrl(model.getConfig().getFirst("domainurl"));
        intgdomain.setClientId(model.getConfig().getFirst("domainclientid"));
        intgdomain.setClientSecret(model.getConfig().getFirst("domainclientsecret"));
        intgdomain.setIdProvider(model.getConfig().getFirst("idprovider"));
        intgdomain.setUsersDn(model.getConfig().getFirst("users_dn"));
        intgdomain.setkeycloakHostname(model.getConfig().getFirst("keycloak_hostname"));

        /* Optional fields */
        String cacert = model.getConfig().getFirst("cacert");
        String extra = model.getConfig().getFirst("extraattrs");
        String oc = model.getConfig().getFirst("user_object_classes");

        if (cacert != null && !cacert.isEmpty()) {
            intgdomain.setLdapTlsCacert(cacert);
        }
        if (extra != null && !extra.isEmpty()) {
            intgdomain.setUserExtraAttrs(extra);
        }
        if (oc != null && !oc.isEmpty()) {
            List<String> oclist = Arrays.asList(oc.split("\\s*,\\s*"));
            intgdomain.setUserObjectClasses(oclist);
        }

        return intgdomain;
    }

    private SCIMUser getUserByAttr(String username, String attribute) {
        SCIMSearchRequest newSearch = setupSearch(username, attribute);

        String usersSearchUrl = "Users/.search";
        SCIMUser user = null;

        SimpleHttp.Response response;
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

    public SimpleHttp.Response deleteUser(String username) {
        SCIMUser userobj = getUserByUsername(username);
        SCIMUser.Resource user = userobj.getResources().get(0);

        String userIdUrl = String.format("Users/%s", user.getId());

        SimpleHttp.Response response;
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

    public SimpleHttp.Response createUser(String username) {
        String usersUrl = "Users";

        SCIMUser.Resource newUser = setupUser(username);

        SimpleHttp.Response response;
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
            case "firstName":
                name.setGivenName(value);
                user.setName(name);
                break;
            case "lastName":
                name.setFamilyName(value);
                user.setName(name);
                break;
            case "email":
                email.setValue(value);
                emails.add(email);
                user.setEmails(emails);
                break;
            case "userName":
                /* Changing username not supported */
                break;
            default:
                logger.info("Unknown user attribute to set: " + attr);
                break;
        }
    }

    public SimpleHttp.Response updateUser(Scim scim, String username, String attr, List<String> values) {
        logger.info(String.format("Updating %s attribute for %s", attr, username));
        /* Get existing user */
        if (scim.csrfAuthLogin() == null) {
            logger.error("Error during login");
        }

        SCIMUser userobj = getUserByUsername(username);
        SCIMUser.Resource user = userobj.getResources().get(0);

        /* Modify attributes */
        setUserAttr(user, attr, values.get(0));

        /* Update user in SCIM */
        String modifyUrl = String.format("Users/%s", user.getId());

        SimpleHttp.Response response;
        try {
            response = clientRequest(modifyUrl, "PUT", user);
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        return response;
    }

    public boolean getActive(SCIMUser user) {
        return Boolean.valueOf(user.getResources().get(0).getActive());
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
        List<SCIMUser.Resource.Group> groups = new ArrayList<SCIMUser.Resource.Group>();
        List<String> groupnames = new ArrayList<String>();
        groups = user.getResources().get(0).getGroups();

        for (int i = 0; i < groups.size(); i++) {
            logger.info("Retrieving group: " + groups.get(i).getDisplay());
            groupnames.add(groups.get(i).getDisplay());
        }

        return groupnames;
    }
}
