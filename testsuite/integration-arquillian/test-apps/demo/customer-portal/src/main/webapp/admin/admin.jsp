<%@ page import="org.keycloak.example.AdminClient" %>
<%@ page import="org.keycloak.representations.idm.RoleRepresentation" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<html>
<head>
    <title>Customer Admin Interface</title>
</head>
<body bgcolor="#E3F6CE">
<h1>Customer Admin Interface</h1>
User <b><%=request.getUserPrincipal().getName()%>
</b> made this request.
<p>

</p>
<h2>Admin REST To Get Role List of Realm</h2>
<%
    java.util.List<RoleRepresentation> list = null;
    try {
        list = AdminClient.getRealmRoles(request);
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