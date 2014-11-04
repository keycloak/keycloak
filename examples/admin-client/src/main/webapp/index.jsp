<%@ page import="org.keycloak.admin.client.Keycloak" %>
<%@ page import="org.keycloak.admin.client.resource.ApplicationsResource" %>
<%@ page import="org.keycloak.representations.idm.ApplicationRepresentation" %>
<%@ page import="org.keycloak.util.UriUtils" %>
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
    ApplicationsResource applications = keycloak.realm("example").applications();

    out.println("<h1>Applications</h1>");
    out.println("<ul>");
    for (ApplicationRepresentation app : applications.findAll()) {
        out.println("\t<li>");
        if (app.getBaseUrl() != null) {
            out.println("\t\t<a href=\"" + app.getBaseUrl() + "\">" + app.getName() + "</a>");
        } else {
            out.println("\t\t" + app.getName());
        }
        out.println("</li>");
    }
    out.println("</ul>");
%>
<br><br>
</body>
</html>
