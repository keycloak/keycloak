<%@ page import="org.keycloak.example.oauth.ProductDatabaseClient" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<html>
<head>
    <title>Pull Page</title>
</head>
<body>
<h2>Pulled Product Listing</h2>
<%
    java.util.List<String> list = null;
    try {
        list = ProductDatabaseClient.getProducts(request);
    } catch (ProductDatabaseClient.Failure failure) {
        out.println("There was a failure processing request.  You either didn't configure Keycloak properly, or maybe" +
                "you just forgot to secure the database service?");
        out.println("Status from database service invocation was: " + failure.getStatus());
        return;
    }
    for (String prod : list)
{
   out.print("<p>");
   out.print(prod);
   out.println("</p>");

}
%>
<br><br>
</body>
</html>