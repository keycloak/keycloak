<%@ page import="javax.ws.rs.core.*" language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<html>
<head>
    <title>Customer Portal</title>
</head>
<body bgcolor="#E3F6CE">
<%
   if (request.getUserPrincipal() != null) {
      String logoutUri = UriBuilder.fromUri("http://localhost:8080/auth-server/rest/realms/demo/tokens/logout")
                                     .queryParam("redirect_uri", "http://localhost:8080/customer-portal").build().toString();
%>
   <p> <%=request.getUserPrincipal().getName()%> | <a href="<%=logoutUri%>">logout</a></p>
<%
}
%>
<h1>Customer Portal</h1>

<p><a href="customers/view.jsp">Customer Listing</a></p>
<p><a href="admin/admin.jsp">Customer Admin Interface</a></p>

</body>
</html>

