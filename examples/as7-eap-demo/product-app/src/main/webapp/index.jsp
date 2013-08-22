<%@ page import="javax.ws.rs.core.*" language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<html>
<head>
    <title>Product Portal</title>
</head>
<body bgcolor="#F5F6CE">
<%
   if (request.getUserPrincipal() != null) {
      String logoutUri = UriBuilder.fromUri("http://localhost:8080/auth-server/rest/realms/demo/tokens/logout")
                                     .queryParam("redirect_uri", "http://localhost:8080/product-portal").build().toString();
%>
   <p> <%=request.getUserPrincipal().getName()%> | <a href="<%=logoutUri%>">logout</a></p>
<%
   }
%>
<h1>Product Portal</h1>

<p><a href="products/view.jsp">Product Listing</a></p>
<p><a href="admin/admin.jsp">Admin Interface</a></p>

</body>
</html>
