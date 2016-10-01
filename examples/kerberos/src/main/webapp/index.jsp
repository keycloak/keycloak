<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.example.kerberos.GSSCredentialsClient" %>
<%@ page session="false" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Kerberos Credentials Delegation Example</title>
    </head>
    <body bgcolor="#ffffff">
        <h1>Kerberos Credentials Delegation Example</h1>
        <hr />

<%
    String logoutUri = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/kerberos-portal").build("kerberos-demo").toString();
%>
        <b>Details about user from LDAP</b> | <a href="<%=logoutUri%>">Logout</a><br />
        <hr />
<%
    try {
        GSSCredentialsClient.LDAPUser ldapUser = GSSCredentialsClient.getUserFromLDAP(request);
        out.println("<p>uid: <b>" + ldapUser.getUid() + "</b></p>");
        out.println("<p>cn: <b>" + ldapUser.getCn() + "</b></p>");
        out.println("<p>sn: <b>" + ldapUser.getSn() + "</b></p>");
    } catch (Exception e) {
        e.printStackTrace();
        out.println("<b>There was a failure in retrieve GSS credential or invoking LDAP. Check server.log for more details</b>");
    }
%>
    </body>
</html>