<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<%@ page import="org.keycloak.example.oauth.ProductDatabaseClient" %>
<%@ page import="org.keycloak.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.ServiceUrlConstants" %>
<html>
<head>
    <title>Servlet Logout</title>
</head>
<body bgcolor="#F5F6CE">
Performs a servlet logout
    <%
    request.logout();
%>
</body>
</html>