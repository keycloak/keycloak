<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.example.CamelClient" %>
<html>
<head>
  <title>Camel page</title>
</head>
<body bgcolor="#E3F6CE">
<p>You will receive info from camel endpoint. Endpoint is accessible just for user with admin role</p>
<p>Response from camel: <b><%= CamelClient.sendRequest(request) %></b> </p>
    <br><br>
  </body>
</html>