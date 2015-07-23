<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.example.ProductServiceAccountServlet" %>
<%@ page import="org.keycloak.representations.AccessToken" %>
<%@ page import="org.keycloak.constants.ServiceAccountConstants" %>
<%@ page import="org.keycloak.util.Time" %>
<html>
<head>
    <title>Service account portal</title>
</head>
<body bgcolor="#FFFFFF">
<%
    AccessToken token = (AccessToken) request.getSession().getAttribute(ProductServiceAccountServlet.TOKEN_PARSED);
    String products = (String) request.getAttribute(ProductServiceAccountServlet.PRODUCTS);
    String appError = (String) request.getAttribute(ProductServiceAccountServlet.ERROR);
%>
<h1>Service account portal</h1>
<p><a href="/service-account-portal/app/login">Login</a> | <a href="/service-account-portal/app/refresh">Refresh token</a> | <a
        href="/service-account-portal/app/logout">Logout</a></p>
<hr />

<% if (appError != null) { %>
    <p><font color="red">
        <b>Error: </b> <%= appError %>
    </font></p>
    <hr />
<% } %>

<% if (token != null) { %>
    <p>
        <b>Service account available</b><br />
        Client ID: <%= token.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID) %><br />
        Client hostname: <%= token.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST) %><br />
        Client address: <%= token.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS) %><br />
        Token expiration: <%= Time.toDate(token.getExpiration()) %><br />
        <% if (token.isExpired()) { %>
            <font color="red">Access token is expired. You may need to refresh</font><br />
        <% } %>
    </p>
    <hr />
<% } %>

<% if (products != null) { %>
    <p>
        <b>Products retrieved successfully from REST endpoint</b><br />
        Product list: <%= products %>
    </p>
    <hr />
<% } %>

</body>
</html>