<%@ page import="javax.ws.rs.core.UriBuilder" language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<html>
<head>
    <title>Customer View Page</title>
</head>
<body bgcolor="#E3F6CE">
<%
   String logoutUri = UriBuilder.fromUri("http://localhost:8080/auth-server/rest/realms/demo/tokens/logout")
                                     .queryParam("redirect_uri", "http://localhost:8080/customer-portal").build().toString();
%>
<p>Goto: <a href="http://localhost:8080/product-portal">products</a> | <a href="<%=logoutUri%>">logout</a></p>
User <b><%=request.getUserPrincipal().getName()%></b> made this request.
<h2>Customer Listing</h2>
<%
java.util.List<String> list = org.jboss.resteasy.example.oauth.CustomerDatabaseClient.getCustomers(request);
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