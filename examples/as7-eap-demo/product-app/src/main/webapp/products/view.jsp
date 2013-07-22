<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<html>
<head>
    <title>Product View Page</title>
</head>
<body bgcolor="#F5F6CE">
<p>Goto: <a href="https://localhost:8443/customer-portal">customers</a> | <a href="https://localhost:8443/auth-server/j_oauth_logout">logout</a></p>
User <b><%=request.getUserPrincipal().getName()%></b> made this request.
<h2>Product Listing</h2>
<%
java.util.List<String> list = org.jboss.resteasy.example.oauth.ProductDatabaseClient.getProducts(request);
for (String cust : list)
{
   out.print("<p>");
   out.print(cust);
   out.println("</p>");

}
%>
<br><br>
</body>
</html>