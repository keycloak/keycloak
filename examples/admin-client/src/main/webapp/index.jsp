<%@ page import="org.keycloak.admin.client.Keycloak" %>
<%@ page import="org.keycloak.admin.client.resource.ClientsResource" %>
<%@ page import="org.keycloak.common.util.UriUtils" %>
<%@ page import="org.keycloak.representations.idm.ClientRepresentation" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<html>
<head>
    <title>Applications</title>
</head>
<body>
<%
    String authServer = UriUtils.getOrigin(request.getRequestURL().toString()) + "/auth";

    Keycloak keycloak = Keycloak.getInstance(authServer, "example", "examples-admin-client", "password", "examples-admin-client", "password");
    ClientsResource clients = keycloak.realm("example").clients();

    out.println("<h1>Applications</h1>");
    out.println("<ul>");
    for (ClientRepresentation client : clients.findAll()) {
        out.println("\t<li>");
        if (client.getBaseUrl() != null) {
            out.println("\t\t<a href=\"" + client.getBaseUrl() + "\">" + client.getClientId() + "</a>");
        } else {
            out.println("\t\t" + client.getClientId());
        }
        out.println("</li>");
    }
    out.println("</ul>");
%>
<br><br>
</body>
</html>
