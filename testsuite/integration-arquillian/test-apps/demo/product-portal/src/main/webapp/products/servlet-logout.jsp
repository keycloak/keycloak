<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<%@ page session="false" %>
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