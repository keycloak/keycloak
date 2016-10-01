<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.common.util.HostUtils" %>
<%@ page import="org.keycloak.example.CustomerDatabaseClient" %>
<html>
<head>
  <title>Customer Session Page</title>
</head>
<body bgcolor="#E3F6CE">
<p>Your hostname: <b><%= HostUtils.getHostName() %></b></p>
    <p>Your session ID: <b><%= request.getSession().getId() %></b></p>
    <p>You visited this page <b><%= CustomerDatabaseClient.increaseAndGetCounter(request) %></b> times.</p>
    <br><br>
  </body>
</html>