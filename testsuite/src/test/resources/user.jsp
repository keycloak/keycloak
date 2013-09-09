<%@ page import="javax.ws.rs.core.*" language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<html>
<head>
<title>User</title>
</head>
<body>
	<%
	    String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "not logged in";
	    String redirectUri = request.getRequestURL().toString();
	%>
	<a href="http://localhost:8080/auth-server/rest/realms/demo/tokens/logout?redirect_uri=<%=redirectUri%>" id="logout">logout</a>
	<span id="user"><%=user%></span>
</body>
</html>