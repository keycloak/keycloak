<%@ page import="org.keycloak.example.oauth.ProductDatabaseClient" %>
<%@ page session="false" %>
<%
   ProductDatabaseClient.redirect(request, response);
%>