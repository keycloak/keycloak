<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<html>
<head>
    <title>Pull Page</title>
</head>
<body>
<h2>Pulled Product Listing</h2>
<%
java.util.List<String> list = org.jboss.resteasy.example.oauth.ProductDatabaseClient.getProducts(request);
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