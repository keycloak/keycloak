<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.example.CustomerDatabaseClient" %>
<%@ page import="org.keycloak.representations.IDToken" %>
<%@ page import="org.keycloak.representations.UserClaimSet" %>
<%@ page import="org.keycloak.util.KeycloakUriBuilder" %>
<%@ page session="false" %>
<html>
<head>
    <title>Customer View Page</title>
</head>
<body bgcolor="#E3F6CE">
<%
    String logoutUri = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/customer-portal").build("demo").toString();
    String acctUri = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH)
            .queryParam("referrer", "customer-portal").build("demo").toString();
    IDToken idToken = CustomerDatabaseClient.getIDToken(request);
    UserClaimSet claims = idToken.getUserClaimSet();
%>
<p>Goto: <a href="/product-portal">products</a> | <a href="<%=logoutUri%>">logout</a> | <a
        href="<%=acctUri%>">manage acct</a></p>
Servlet User Principal <b><%=request.getUserPrincipal().getName()%>
</b> made this request.
<p><b>Caller IDToken values</b> (<i>You can specify what is returned in IDToken in the customer-portal claims page in the admin console</i>:</p>
<p>Username: <%=claims.getPreferredUsername()%></p>
<p>Email: <%=claims.getEmail()%></p>
<p>Full Name: <%=claims.getName()%></p>
<p>First: <%=claims.getGivenName()%></p>
<p>Last: <%=claims.getFamilyName()%></p>
<h2>Customer Listing</h2>
<%
    java.util.List<String> list = null;
    try {
        list = CustomerDatabaseClient.getCustomers(request);
    } catch (CustomerDatabaseClient.Failure failure) {
        out.println("There was a failure processing request.  You either didn't configure Keycloak properly, or maybe" +
                "you just forgot to secure the database service?");
        out.println("Status from database service invocation was: " + failure.getStatus());
        return;
    }
    for (String cust : list) {
        out.print("<p>");
        out.print(cust);
        out.println("</p>");

    }
%>
<br><br>
</body>
</html>