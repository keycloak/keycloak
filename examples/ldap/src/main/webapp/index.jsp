<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.representations.AccessToken" %>
<%@ page import="org.keycloak.representations.AccessToken.Access" %>
<%@ page import="org.keycloak.representations.IDToken" %>
<%@ page import="java.util.Map" %>
<%@ page session="false" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>LDAP Example</title>
    </head>
    <body bgcolor="#ffffff">
        <h1>LDAP Example</h1>
        <hr />

<%
    String logoutUri = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .build("ldap-demo").toString();

    KeycloakSecurityContext securityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    IDToken idToken = securityContext.getIdToken();
    AccessToken accessToken = securityContext.getToken();
%>
        <a href="<%=logoutUri%>">Logout</a><br />
        <hr />


        <h2>ID Token - basic claims</h2>
        <p><b>Username: </b><%=idToken.getPreferredUsername()%></p>
        <p><b>Email: </b><%=idToken.getEmail()%></p>
        <p><b>Full Name: </b><%=idToken.getName()%></p>
        <p><b>First: </b><%=idToken.getGivenName()%></p>
        <p><b>Last: </b><%=idToken.getFamilyName()%></p>
        <% if (idToken.getPicture() != null) { %>
            <p><b>Profile picture: </b><img src='/ldap-portal/picture' /></p>
        <% } %>
        <hr />


        <h2>ID Token - other claims</h2>
<%
    for (Map.Entry<String, Object> claim : idToken.getOtherClaims().entrySet()) {
%>
        <p><b><%= claim.getKey() %>: </b><%= claim.getValue().toString() %>
<%
    }
%>
        <hr />


        <h2>Access Token - roles</h2>
        <p><b>Realm roles: </b><%= accessToken.getRealmAccess().getRoles().toString() %></p>
<%
    for (Map.Entry<String, Access> acc : accessToken.getResourceAccess().entrySet()) {
%>
        <p><b>Resource: </b><%= acc.getKey() %>, <b>Roles: </b><%= acc.getValue().getRoles().toString() %></p>
<%
    }
%>
        <hr />

    </body>
</html>