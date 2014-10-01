<%@ page import="org.keycloak.example.AdminClient" %>
<%@ page import="org.keycloak.representations.AccessTokenResponse" %>
<%@ page import="org.keycloak.representations.idm.RoleRepresentation" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<html>
<head>
    <title>Admin Interface</title>
</head>
<body bgcolor="#E3F6CE">
<h2>List of Realm Roles from Admin REST API Call</h2>
<%
    java.util.List<RoleRepresentation> list = null;
    try {
        AccessTokenResponse res = AdminClient.getToken(request);
        list = AdminClient.getRealmRoles(request, res);
        AdminClient.logout(request, res);
    } catch (AdminClient.Failure failure) {
        out.println("There was a failure processing request.  You either didn't configure Keycloak properly");
        out.println("Status from database service invocation was: " + failure.getStatus());
        return;
    }
    for (RoleRepresentation role : list) {
        out.print("<p>");
        out.print(role.getName());
        out.println("</p>");

    }
%></body>
</html>