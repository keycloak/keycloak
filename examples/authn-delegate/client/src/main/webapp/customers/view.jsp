<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.common.util.Time" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.example.authn.delegation.client.AuthenticatedUser" %>
<%@ page import="org.keycloak.representations.IDToken" %>
<%@ page session="false" %>
<html>
<head>
    <title>Authn-Delegation Authenticated User's View Page</title>
</head>
<body bgcolor="#D0DA96">
<%
    String logoutUri = KeycloakUriBuilder.fromUri("http://localhost:8080/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "http://localhost:8380/authn-delegation-client").build("authn-delegation").toString();
    IDToken idToken = AuthenticatedUser.getIDToken(request);
%>
<p>Goto: <a href="<%=logoutUri%>">logout</a></p>
Servlet User Principal <b><%=request.getUserPrincipal().getName()%>
</b> made this request.
<p><b>Caller IDToken values</b>:</p>
<p>Username: <%=idToken.getPreferredUsername()%></p>
<p>Email: <%=idToken.getEmail()%></p>
<p>Full Name: <%=idToken.getName()%></p>
<p>First: <%=idToken.getGivenName()%></p>
<p>Last: <%=idToken.getFamilyName()%></p>
<p>Nonce: <%=idToken.getNonce()%></p>
<p>Authenticated Time: <%=Time.toDate((int)idToken.getAuthTime())%></p>
<p>Session State: <%=idToken.getSessionState()%></p>
<p>Authentication Context Class Reference: <%=idToken.getAcr()%></p>
<p>Updated at: <%=(idToken.getUpdatedAt() != null) ? Time.toDate((int)idToken.getUpdatedAt().longValue()) : null%></p>
<p>Authorization Code Hash: <%=idToken.getCodeHash()%></p>
<p>Access Token Hash: <%=idToken.getAccessTokenHash()%></p>
<p>Locale: <%=idToken.getLocale()%></p>
<p>Claims Locales: <%=idToken.getClaimsLocales()%></p>
<p>Middle Name: <%=idToken.getMiddleName()%></p>
<p>Nick Name: <%=idToken.getNickName()%></p>
<p>Profile: <%=idToken.getProfile()%></p>
<p>Picture: <%=idToken.getPicture()%></p>
<p>Web Site: <%=idToken.getWebsite()%></p>
<p>Gender: <%=idToken.getGender()%></p>
<p>Birth Date: <%=idToken.getBirthdate()%></p>
<p>Zone Info: <%=idToken.getZoneinfo()%></p>
<p>Phone Number: <%=idToken.getPhoneNumber()%></p>
<p></p>
<p><b>Caller JWT values</b>:</p>
<p>ID: <%=idToken.getId()%></p>
<p>Expiration: <%=Time.toDate((int)idToken.getExpiration())%></p>
<p>Expired?: <%=idToken.isExpired()%></p>
<p>Not Before: <%=Time.toDate((int)idToken.getNotBefore())%></p>
<p>Not Before?: <%=idToken.isNotBefore()%></p>
<p>Active?: <%=idToken.isActive()%></p>
<p>Issued at: <%=Time.toDate((int)idToken.getIssuedAt())%></p>
<p>Issuer: <%=idToken.getIssuer()%></p>
<p>Audience: <%=idToken.getAudience()%></p>
<p>Subject: <%=idToken.getSubject()%></p>
<p>Type: <%=idToken.getType()%></p>
<p>Issued For: <%=idToken.getIssuedFor()%></p>
<p>Other Claims: <%=idToken.getOtherClaims()%></p>
<p></p>
<p>Your session ID: <b><%= request.getSession().getId() %></b></p>
<br><br>
</body>
</html>